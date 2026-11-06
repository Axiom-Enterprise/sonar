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

import com.github.benmanes.caffeine.cache.Cache;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.database.model.BlacklistedAddress;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Keeps the in-memory blacklist in sync with the database, so blacklisted addresses survive restarts.
 * <p>
 * All database operations run asynchronously on a single thread to never block the Netty event loop.
 * Only addresses that actually reached the blacklist threshold are written, which keeps the number of
 * writes low even during large attacks.
 * <p>
 * Trade-off: a restored entry gets the full blacklist time again in the Caffeine cache,
 * so an address can stay blacklisted up to twice as long as configured across a restart.
 */
public final class BlacklistController {
  private final @Nullable Dao<BlacklistedAddress, String> dao;
  private final ExecutorService updateService = Executors.newSingleThreadExecutor();

  /**
   * @param connectionSource The shared database connection or {@code null} if persistence is disabled
   */
  public BlacklistController(final @Nullable ConnectionSource connectionSource) {
    Dao<BlacklistedAddress, String> createdDao = null;
    if (connectionSource != null) {
      try {
        TableUtils.createTableIfNotExists(connectionSource, BlacklistedAddress.class);
        createdDao = DaoManager.createDao(connectionSource, BlacklistedAddress.class);
      } catch (SQLException exception) {
        Sonar.get0().getLogger().error("Could not prepare the blacklist table: {}", exception);
      }
    }
    this.dao = createdDao;
  }

  /**
   * @return {@code true} if blacklist entries are stored in the database
   */
  public boolean isEnabled() {
    return dao != null;
  }

  /**
   * Deletes expired entries and asynchronously restores all remaining entries into the given cache.
   *
   * @param blacklist     The in-memory blacklist cache
   * @param blacklistTime The configured blacklist duration in milliseconds
   */
  public void load(final @NotNull Cache<String, Integer> blacklist, final long blacklistTime) {
    execute(dao -> {
      final Timestamp oldest = new Timestamp(System.currentTimeMillis() - blacklistTime);
      // Remove entries that would have already expired from the cache
      final DeleteBuilder<BlacklistedAddress, String> deleteBuilder = dao.deleteBuilder();
      deleteBuilder.where().lt(BlacklistedAddress.TIMESTAMP_COLUMN, oldest);
      deleteBuilder.delete();
      // Keep the highest score if the address is already in the cache (e.g. after a reload)
      int restored = 0;
      for (final BlacklistedAddress entry : dao.queryForAll()) {
        blacklist.asMap().merge(entry.getAddress(), entry.getScore(), Math::max);
        restored++;
      }
      if (restored > 0) {
        Sonar.get0().getLogger().info("Restored {} blacklisted addresses from the database.", restored);
      }
    });
  }

  /**
   * Asynchronously inserts or updates a blacklist entry
   *
   * @param address The blacklist key
   * @param score   The current score of the address
   */
  public void save(final @NotNull String address, final int score) {
    execute(dao -> dao.createOrUpdate(new BlacklistedAddress(address, score, System.currentTimeMillis())));
  }

  /**
   * Asynchronously removes a blacklist entry
   *
   * @param address The blacklist key
   */
  public void remove(final @NotNull String address) {
    execute(dao -> dao.deleteById(address));
  }

  /**
   * Asynchronously removes all blacklist entries
   */
  public void clear() {
    execute(dao -> dao.deleteBuilder().delete());
  }

  /**
   * Stops accepting new database operations; must be called before the connection is closed
   */
  public void close() {
    updateService.shutdown();
  }

  private void execute(final @NotNull DatabaseTask task) {
    final Dao<BlacklistedAddress, String> currentDao = dao;
    if (currentDao == null || updateService.isShutdown()) {
      return;
    }
    updateService.execute(() -> {
      try {
        task.run(currentDao);
      } catch (SQLException exception) {
        Sonar.get0().getLogger().error("Could not update the blacklist table: {}", exception);
      }
    });
  }

  @FunctionalInterface
  private interface DatabaseTask {
    void run(final @NotNull Dao<BlacklistedAddress, String> dao) throws SQLException;
  }
}
