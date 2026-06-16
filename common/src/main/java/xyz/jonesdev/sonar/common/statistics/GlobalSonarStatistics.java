/*
 * Copyright (C) 2025 Sonar Contributors
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

package xyz.jonesdev.sonar.common.statistics;

import org.jetbrains.annotations.ApiStatus;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.profiler.SimpleProcessProfiler;
import xyz.jonesdev.sonar.api.statistics.SonarStatistics;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public final class GlobalSonarStatistics implements SonarStatistics {
  private static final LongAdder CONNECTIONS_THIS_SECOND = new LongAdder();
  private static final LongAdder LOGINS_THIS_SECOND = new LongAdder();
  private static volatile long connectionsPerSecond;
  private static volatile long loginsPerSecond;

  /**
   * Helper methods that make it easier to count new statistics
   */

  @ApiStatus.Internal
  public static void countConnection() {
    CONNECTIONS_THIS_SECOND.increment();
  }

  @ApiStatus.Internal
  public static void countLogin() {
    LOGINS_THIS_SECOND.increment();
    totalJoinedPlayers.incrementAndGet();
  }

  // Cache all per-session statistics
  private static final AtomicInteger totalJoinedPlayers = new AtomicInteger();
  public static final AtomicInteger totalSuccessfulVerifications = new AtomicInteger();
  public static final AtomicInteger totalFailedVerifications = new AtomicInteger();
  public static final AtomicInteger totalAttemptedVerifications = new AtomicInteger();
  public static final AtomicLong totalBlacklistedPlayers = new AtomicLong();
  public static final AtomicLong totalIncomingTraffic = new AtomicLong();
  public static final AtomicLong totalOutgoingTraffic = new AtomicLong();
  public static final AtomicLong perSecondIncomingTraffic = new AtomicLong();
  public static final AtomicLong perSecondOutgoingTraffic = new AtomicLong();
  private static volatile String perSecondIncomingTrafficFormatted;
  private static volatile String perSecondOutgoingTrafficFormatted;

  public static void cleanUpCaches() {
    // no-op: caches replaced with LongAdder counters
  }

  public static void hitEverySecond() {
    connectionsPerSecond = CONNECTIONS_THIS_SECOND.sumThenReset();
    loginsPerSecond = LOGINS_THIS_SECOND.sumThenReset();
    final long incoming = perSecondIncomingTraffic.getAndSet(0L);
    final long outgoing = perSecondOutgoingTraffic.getAndSet(0L);
    totalIncomingTraffic.addAndGet(incoming);
    totalOutgoingTraffic.addAndGet(outgoing);
    perSecondIncomingTrafficFormatted = SimpleProcessProfiler.formatMemory(incoming);
    perSecondOutgoingTrafficFormatted = SimpleProcessProfiler.formatMemory(outgoing);
  }

  @Override
  public long getConnectionsPerSecond() {
    return connectionsPerSecond;
  }

  @Override
  public long getLoginsPerSecond() {
    return loginsPerSecond;
  }

  @Override
  public long getCurrentIncomingBandwidth() {
    return perSecondIncomingTraffic.get();
  }

  @Override
  public long getCurrentOutgoingBandwidth() {
    return perSecondOutgoingTraffic.get();
  }

  @Override
  public long getTotalIncomingBandwidth() {
    return totalIncomingTraffic.get();
  }

  @Override
  public long getTotalOutgoingBandwidth() {
    return totalOutgoingTraffic.get();
  }

  @Override
  public String getPerSecondIncomingBandwidthFormatted() {
    return perSecondIncomingTrafficFormatted;
  }

  @Override
  public String getPerSecondOutgoingBandwidthFormatted() {
    return perSecondOutgoingTrafficFormatted;
  }

  @Override
  public int getTotalPlayersJoined() {
    return totalJoinedPlayers.get();
  }

  @Override
  public int getTotalPlayersVerified() {
    return Sonar.get0().getVerifiedPlayerController().getCache().size();
  }

  @Override
  public int getTotalSuccessfulVerifications() {
    return totalSuccessfulVerifications.get();
  }

  @Override
  public int getTotalFailedVerifications() {
    return totalFailedVerifications.get();
  }

  @Override
  public long getCurrentAttemptedVerifications() {
    return Sonar.get0().getAntiBot().getConnected().size();
  }

  @Override
  public int getTotalAttemptedVerifications() {
    return totalAttemptedVerifications.get();
  }

  @Override
  public long getCurrentBlacklistSize() {
    return Sonar.get0().getAntiBot().getBlacklist().estimatedSize();
  }

  @Override
  public long getTotalBlacklistSize() {
    return totalBlacklistedPlayers.get();
  }
}
