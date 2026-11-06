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
 * Persisted blacklist entry; the address is the blacklist key (single IP or subnet, e.g. {@code 2001:db8:0:0:0:0:0:0/64}).
 */
@DatabaseTable(tableName = "sonar_blacklist")
public final class BlacklistedAddress {
  public static final String ADDRESS_COLUMN = "address";
  public static final String TIMESTAMP_COLUMN = "timestamp";

  @DatabaseField(
    columnName = ADDRESS_COLUMN,
    id = true,
    canBeNull = false,
    width = 64
  )
  private String address;

  @DatabaseField(
    columnName = "score",
    canBeNull = false
  )
  private int score;

  @DatabaseField(
    columnName = TIMESTAMP_COLUMN,
    canBeNull = false
  )
  private Timestamp timestamp;

  /**
   * Required by ORMLite to create instances through reflection
   */
  BlacklistedAddress() {
  }

  public BlacklistedAddress(final @NotNull String address,
                            final int score,
                            final long timestamp) {
    this.address = address;
    this.score = score;
    this.timestamp = new Timestamp(timestamp);
  }

  public String getAddress() {
    return address;
  }

  public int getScore() {
    return score;
  }

  public Timestamp getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return "BlacklistedAddress(address=" + address + ", score=" + score + ", timestamp=" + timestamp + ")";
  }
}
