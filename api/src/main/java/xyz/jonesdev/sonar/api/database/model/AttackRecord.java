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


package xyz.jonesdev.sonar.api.database.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;

/**
 * Summary of an attack that has been mitigated, shown by {@code /sonar history}.
 */
@DatabaseTable(tableName = "sonar_attack_history")
public final class AttackRecord {
  public static final String START_COLUMN = "start";

  @DatabaseField(generatedId = true)
  private int id;

  @DatabaseField(columnName = START_COLUMN, canBeNull = false)
  private Timestamp start;

  @DatabaseField(columnName = "duration", canBeNull = false)
  private long durationMillis;

  @DatabaseField(columnName = "peak_bps", canBeNull = false)
  private long peakJoinsPerSecond;

  @DatabaseField(columnName = "peak_cps", canBeNull = false)
  private long peakConnectionsPerSecond;

  @DatabaseField(columnName = "verified", canBeNull = false)
  private long verified;

  @DatabaseField(columnName = "failed", canBeNull = false)
  private long failed;

  @DatabaseField(columnName = "blacklisted", canBeNull = false)
  private long blacklisted;

  // Subnets with the most failed verifications, e.g. "1.2.3.0/24 (120), 5.6.7.0/24 (80)"
  @DatabaseField(columnName = "top_subnets", canBeNull = false, width = 512)
  private String topSubnets;

  /**
   * Required by ORMLite to create instances through reflection
   */
  AttackRecord() {
  }

  public AttackRecord(final long start,
                      final long durationMillis,
                      final long peakJoinsPerSecond,
                      final long peakConnectionsPerSecond,
                      final long verified,
                      final long failed,
                      final long blacklisted,
                      final @NotNull String topSubnets) {
    this.start = new Timestamp(start);
    this.durationMillis = durationMillis;
    this.peakJoinsPerSecond = peakJoinsPerSecond;
    this.peakConnectionsPerSecond = peakConnectionsPerSecond;
    this.verified = verified;
    this.failed = failed;
    this.blacklisted = blacklisted;
    // Keep the value inside the column width
    this.topSubnets = topSubnets.length() > 512 ? topSubnets.substring(0, 512) : topSubnets;
  }

  public Timestamp getStart() {
    return start;
  }

  public long getDurationMillis() {
    return durationMillis;
  }

  public long getPeakJoinsPerSecond() {
    return peakJoinsPerSecond;
  }

  public long getPeakConnectionsPerSecond() {
    return peakConnectionsPerSecond;
  }

  public long getVerified() {
    return verified;
  }

  public long getFailed() {
    return failed;
  }

  public long getBlacklisted() {
    return blacklisted;
  }

  public String getTopSubnets() {
    return topSubnets;
  }

  @Override
  public String toString() {
    return "AttackRecord(start=" + start + ", durationMillis=" + durationMillis + ", peakJoinsPerSecond="
      + peakJoinsPerSecond + ", peakConnectionsPerSecond=" + peakConnectionsPerSecond + ", verified=" + verified
      + ", failed=" + failed + ", blacklisted=" + blacklisted + ", topSubnets=" + topSubnets + ")";
  }
}
