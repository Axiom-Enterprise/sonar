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


package xyz.jonesdev.sonar.api.antibot;

import org.jetbrains.annotations.NotNull;

/**
 * Suspicious, but not impossible behavior of a player during the verification.
 * <p>
 * A single signal is never enough to fail the verification since real players can trigger it too.
 * Instead, every signal adds its configured weight to the risk score of the player,
 * and players whose score reaches the threshold have to solve the map captcha.
 */
public enum RiskSignal {
  // The address did not request the server list before joining (outside the hard server list ping check)
  NO_SERVER_LIST_PING("no-server-list-ping"),
  // The address belongs to a datacenter or VPN network (outside the hard datacenter check)
  DATACENTER("datacenter"),
  // Another player from the same subnet is being verified at the same time
  SUBNET_ACTIVITY("subnet-activity"),
  // The ping of the player is higher than the configured suspicious ping
  HIGH_PING("high-ping"),
  // The player uses a view distance that is lower than 5 chunks
  LOW_VIEW_DISTANCE("low-view-distance");

  private final String configKey;

  RiskSignal(final @NotNull String configKey) {
    this.configKey = configKey;
  }

  /**
   * @return The key of the weight inside {@code verification.risk-score.weights}
   */
  public @NotNull String getConfigKey() {
    return configKey;
  }
}
