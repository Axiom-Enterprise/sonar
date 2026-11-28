/*
 * Copyright (C) 2026 Sonar Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */


package xyz.jonesdev.sonar.api.database.controller;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.database.model.AttackRecord;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Keeps the most recent mitigated attacks in memory and, if a database is configured, in the database.
 * <p>
 * All database operations run asynchronously on a single thread to never block the attack tracker.
 */
public final class AttackHistoryController {
  // Newest attack first
  private final LinkedList<AttackRecord> records = new LinkedList<>();
  private final @Nullable Dao<AttackRecord, Integer> dao;
  private final int maxEntries;
  private final ExecutorService updateService = Executors.newSingleThreadExecutor();

  /**
   * @param connectionSource The shared database connection or {@code null} to only keep the history in memory
   * @param maxEntries       Maximum number of attacks to keep; 0 disables the history
   * @param previous         Attacks of the previous controller, kept across reloads without a database
   */
  public AttackHistoryController(final @Nullable ConnectionSource connectionSource,
                                 final int maxEntries,
                                 final @NotNull List<AttackRecord> previous) {
    this.maxEntries = maxEntries;
    Dao<AttackRecord, Integer> createdDao = null;
    if (connectionSource != null && maxEntries > 0) {
      try {
        TableUtils.createTableIfNotExists(connectionSource, AttackRecord.class);
        createdDao = DaoManager.createDao(connectionSource, AttackRecord.class);
      } catch (SQLException exception) {
        Sonar.get0().getLogger().error("Could not prepare the attack history table: {}", exception);
      }
    }
    this.dao = createdDao;

    if (dao == null) {
      synchronized (records) {
        records.addAll(previous.subList(0, Math.min(previous.size(), maxEntries)));
      }
      return;
    }
    execute(dao -> {
      final List<AttackRecord> stored = dao.queryBuilder()
        .orderBy(AttackRecord.START_COLUMN, false)
        .limit((long) maxEntries)
        .query();
      synchronized (records) {
        records.clear();
        records.addAll(stored);
      }
    });
  }

  /**
   * Adds a mitigated attack to the history and removes the oldest attacks above the limit
   *
   * @param record Summary of the attack
   */
  public void save(final @NotNull AttackRecord record) {
    if (maxEntries <= 0) {
      return;
    }
    synchronized (records) {
      records.addFirst(record);
      while (records.size() > maxEntries) {
        records.removeLast();
      }
    }
    execute(dao -> {
      dao.create(record);
      final long excess = dao.countOf() - maxEntries;
      if (excess > 0) {
        dao.delete(dao.queryBuilder()
          .orderBy(AttackRecord.START_COLUMN, true)
          .limit(excess)
          .query());
      }
    });
  }

  /**
   * @return A copy of the stored attacks, newest attack first
   */
  public @NotNull List<AttackRecord> getRecords() {
    synchronized (records) {
      return records.isEmpty() ? Collections.emptyList() : new ArrayList<>(records);
    }
  }

  /**
   * Stops accepting new database operations; must be called before the connection is closed
   */
  public void close() {
    updateService.shutdown();
  }

  private void execute(final @NotNull DatabaseTask task) {
    final Dao<AttackRecord, Integer> currentDao = dao;
    if (currentDao == null || updateService.isShutdown()) {
      return;
    }
    updateService.execute(() -> {
      try {
        task.run(currentDao);
      } catch (SQLException exception) {
        Sonar.get0().getLogger().error("Could not update the attack history table: {}", exception);
      }
    });
  }

  @FunctionalInterface
  private interface DatabaseTask {
    void run(final @NotNull Dao<AttackRecord, Integer> dao) throws SQLException;
  }
}
