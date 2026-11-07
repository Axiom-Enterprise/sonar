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

package xyz.jonesdev.sonar.api.tracker;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;
import xyz.jonesdev.sonar.api.event.impl.AttackDetectedEvent;
import xyz.jonesdev.sonar.api.event.impl.AttackMitigatedEvent;
import xyz.jonesdev.sonar.api.timer.SystemTimer;
import xyz.jonesdev.sonar.api.webhook.DiscordWebhook;
import xyz.jonesdev.sonar.api.webhook.GenericWebhook;

import java.util.LinkedHashMap;
import java.util.Map;

import static xyz.jonesdev.sonar.api.profiler.SimpleProcessProfiler.*;

public final class AttackTracker {
  public static final AttackTracker INSTANCE = new AttackTracker();

  private volatile @Nullable AttackStatistics currentAttack;
  private int attackThreshold;
  // Exponentially weighted moving average of the joins per second outside of attacks
  private volatile double baselineJoinsPerSecond;

  // Weight of a new sample in the baseline; this method runs every 250ms,
  // so the baseline follows changes over roughly one minute
  private static final double BASELINE_SMOOTHING = 0.005;

  public static final class AttackStatistics {
    private final SystemTimer duration = new SystemTimer();
    private final SystemTimer timer = new SystemTimer();
    private long peakJoinsPerSecond;
    private long peakConnectionsPerSecond;
    private double peakProcessCPUUsage;
    private long peakProcessMemoryUsage;
    // Calculate during-attack-statistics using their deltas
    private int successfulVerifications;
    private int failedVerifications;
    // Whether the webhooks have already been notified about the start of this attack
    private boolean startNotified;

    public SystemTimer getDuration() {
      return this.duration;
    }

    public SystemTimer getTimer() {
      return this.timer;
    }

    public long getPeakJoinsPerSecond() {
      return this.peakJoinsPerSecond;
    }

    public long getPeakConnectionsPerSecond() {
      return this.peakConnectionsPerSecond;
    }

    public double getPeakProcessCPUUsage() {
      return this.peakProcessCPUUsage;
    }

    public long getPeakProcessMemoryUsage() {
      return this.peakProcessMemoryUsage;
    }

    public int getSuccessfulVerifications() {
      return this.successfulVerifications;
    }

    public int getFailedVerifications() {
      return this.failedVerifications;
    }

    public boolean isStartNotified() {
      return this.startNotified;
    }

    @java.lang.Override
    public java.lang.String toString() {
      return "AttackTracker.AttackStatistics(duration=" + this.getDuration() + ", timer=" + this.getTimer() + ", peakJoinsPerSecond=" + this.getPeakJoinsPerSecond() + ", peakConnectionsPerSecond=" + this.getPeakConnectionsPerSecond() + ", peakProcessCPUUsage=" + this.getPeakProcessCPUUsage() + ", peakProcessMemoryUsage=" + this.getPeakProcessMemoryUsage() + ", successfulVerifications=" + this.getSuccessfulVerifications() + ", failedVerifications=" + this.getFailedVerifications() + ", startNotified=" + this.isStartNotified() + ")";
    }

    public AttackStatistics() {
    }
  }

  public void checkIfUnderAttack() {
    final long joinsPerSecond = Sonar.get0().getStatistics().getLoginsPerSecond();
    final int verifyingPlayers = Sonar.get0().getAntiBot().getConnected().size();
    final int queuedPlayers = Sonar.get0().getAntiBot().getQueue().getPlayers().size();
    final int minPlayers = getEffectiveMinPlayers();

    // Only learn from normal traffic, so attacks cannot raise their own threshold
    if (currentAttack == null) {
      baselineJoinsPerSecond += BASELINE_SMOOTHING * (joinsPerSecond - baselineJoinsPerSecond);
    }

    if (joinsPerSecond > minPlayers // Check the number of bots/joins per second.
      || verifyingPlayers > minPlayers // Check the number of verifying players.
      || queuedPlayers > minPlayers) { // Check the number of queued players.
      // Increment attack threshold
      ++attackThreshold;

      // An attack has been detected
      if (currentAttack == null) {
        currentAttack = new AttackStatistics();
        currentAttack.successfulVerifications = Sonar.get0().getVerifiedPlayerController().getCache().size();
        currentAttack.failedVerifications = Sonar.get0().getStatistics().getTotalFailedVerifications();
        Sonar.get0().getEventManager().publish(new AttackDetectedEvent());
        Sonar.get0().getChatNotificationHandler().handleNotification();
      } else {
        // Reset attack timer if we're still under attack
        currentAttack.timer.reset();
      }

      // Only notify webhooks once the attack exceeded the threshold, so short spikes don't spam them
      if (!currentAttack.startNotified && attackThreshold > Sonar.get0().getConfig().getMinAttackThreshold()) {
        currentAttack.startNotified = true;
        postAttackStartWebhooks(joinsPerSecond, verifyingPlayers, queuedPlayers);
      }

      // Update attack statistics
      if (joinsPerSecond > currentAttack.peakJoinsPerSecond) {
        // Update joins per second peak if necessary
        currentAttack.peakJoinsPerSecond = joinsPerSecond;
      }
      final long connectionsPerSecond = Sonar.get0().getStatistics().getConnectionsPerSecond();
      if (connectionsPerSecond > currentAttack.peakConnectionsPerSecond) {
        // Update connections per second peak if necessary
        currentAttack.peakConnectionsPerSecond = connectionsPerSecond;
      }
      final double processCPUUsage = getProcessCPUUsage();
      if (processCPUUsage > currentAttack.peakProcessCPUUsage) {
        // Update cpu usage peak if necessary
        currentAttack.peakProcessCPUUsage = processCPUUsage;
      }
      final long processMemoryUsage = getUsedMemory();
      if (processMemoryUsage > currentAttack.peakProcessMemoryUsage) {
        // Update memory consumption peak if necessary
        currentAttack.peakProcessMemoryUsage = processMemoryUsage;
      }
    } else if (currentAttack != null) {
      if (currentAttack.duration.delay() > Sonar.get0().getConfig().getMinAttackDuration()
        && currentAttack.timer.delay() > Sonar.get0().getConfig().getAttackCooldownDelay()) {
        // The current attack has stopped
        Sonar.get0().getEventManager().publish(new AttackMitigatedEvent(currentAttack));

        if (++attackThreshold > Sonar.get0().getConfig().getMinAttackThreshold()) {
          postAttackEndWebhooks(currentAttack);
        }

        // Reset the attack status
        currentAttack = null;
        attackThreshold = 0;
      }
    } else {
      attackThreshold = 0;
    }
  }

  /**
   * Returns the number of players that has to be exceeded for an attack to be detected.
   * With the adaptive detection enabled, this is the learned baseline times the configured multiplier,
   * but never less than the configured minimum.
   *
   * @return The effective minimum number of players for an attack
   */
  public int getEffectiveMinPlayers() {
    final int minPlayers = Sonar.get0().getConfig().getMinPlayersForAttack();
    if (!Sonar.get0().getConfig().isAdaptiveAttackDetection()) {
      return minPlayers;
    }
    final double adaptive = Math.ceil(baselineJoinsPerSecond * Sonar.get0().getConfig().getAdaptiveAttackMultiplier());
    return (int) Math.max(minPlayers, Math.min(adaptive, Integer.MAX_VALUE));
  }

  /**
   * Notifies Discord (if the start embed is enabled) and the generic webhook that an attack has started.
   * This method only runs once per attack, so no additional cooldown is needed.
   */
  private static void postAttackStartWebhooks(final long joinsPerSecond,
                                              final int verifyingPlayers,
                                              final int queuedPlayers) {
    final SonarConfiguration.Webhook config = Sonar.get0().getConfig().getWebhook();
    final long connectionsPerSecond = Sonar.get0().getStatistics().getConnectionsPerSecond();
    final String startTimestamp = String.valueOf(System.currentTimeMillis() / 1000L);

    final DiscordWebhook discordWebhook = config.getDiscordWebhook();
    if (discordWebhook != null && config.isStartEmbedEnabled()) {
      discordWebhook.post(() -> {
        final SonarConfiguration.Webhook.Embed embed = config.getStartEmbed();
        final String description = embed.getDescription()
          .replace("<start-timestamp>", startTimestamp)
          .replace("<bps>", Sonar.DECIMAL_FORMAT.format(joinsPerSecond))
          .replace("<cps>", Sonar.DECIMAL_FORMAT.format(connectionsPerSecond))
          .replace("<verifying>", Sonar.DECIMAL_FORMAT.format(verifyingPlayers))
          .replace("<queued>", Sonar.DECIMAL_FORMAT.format(queuedPlayers));
        return new SonarConfiguration.Webhook.Embed(
          embed.getTitle(), embed.getTitleUrl(), description, embed.getR(), embed.getG(), embed.getB());
      });
    }

    final GenericWebhook genericWebhook = config.getGenericWebhook();
    if (genericWebhook != null) {
      final Map<String, Object> data = new LinkedHashMap<>();
      data.put("start-timestamp", Long.parseLong(startTimestamp));
      data.put("bps", joinsPerSecond);
      data.put("cps", connectionsPerSecond);
      data.put("verifying", verifyingPlayers);
      data.put("queued", queuedPlayers);
      genericWebhook.post(GenericWebhook.ATTACK_START, data);
    }
  }

  /**
   * Notifies Discord and the generic webhook that an attack has been mitigated.
   *
   * @param attack Statistics of the attack that just ended
   */
  private static void postAttackEndWebhooks(final @NotNull AttackStatistics attack) {
    final DiscordWebhook discordWebhook = Sonar.get0().getConfig().getWebhook().getDiscordWebhook();
    final GenericWebhook genericWebhook = Sonar.get0().getConfig().getWebhook().getGenericWebhook();
    if (discordWebhook == null && genericWebhook == null) {
      return;
    }

    // Save attack statistics
    final long deltaInMillis = attack.duration.delay();
    final long minutes = deltaInMillis / (60 * 1000); // Convert milliseconds to minutes
    final double seconds = (deltaInMillis % (60 * 1000)) / 1000D; // Convert remaining milliseconds to seconds
    final String formattedDuration = String.format("%d minutes, %.0f seconds", minutes, seconds);
    final long startTimestamp = attack.duration.getStart() / 1000L;
    final long endTimestamp = System.currentTimeMillis() / 1000L;
    final long blacklisted = Sonar.get0().getAntiBot().getBlacklist().estimatedSize();
    // Calculate during-attack-statistics using their deltas
    final long totalVerified = Sonar.get0().getVerifiedPlayerController().getCache().size();
    final long verified = Math.max(totalVerified - attack.successfulVerifications, 0);
    final long totalFailed = Sonar.get0().getStatistics().getTotalFailedVerifications();
    final long failed = Math.max(totalFailed - attack.failedVerifications, 0);

    if (discordWebhook != null) {
      final String peakCPU = Sonar.DECIMAL_FORMAT.format(attack.peakProcessCPUUsage);
      final String peakMem = formatMemory(attack.peakProcessMemoryUsage);
      final String peakBPS = Sonar.DECIMAL_FORMAT.format(attack.peakJoinsPerSecond);
      final String peakCPS = Sonar.DECIMAL_FORMAT.format(attack.peakConnectionsPerSecond);

      discordWebhook.post(() -> {
        final SonarConfiguration.Webhook.Embed config = Sonar.get0().getConfig().getWebhook().getEmbed();
        final String description = config.getDescription()
          .replace("<start-timestamp>", String.valueOf(startTimestamp))
          .replace("<end-timestamp>", String.valueOf(endTimestamp))
          .replace("<attack-duration>", formattedDuration)
          .replace("<peak-cpu>", peakCPU)
          .replace("<peak-memory>", peakMem)
          .replace("<peak-bps>", peakBPS)
          .replace("<peak-cps>", peakCPS)
          .replace("<total-blacklisted>", Sonar.DECIMAL_FORMAT.format(blacklisted))
          .replace("<total-failed>", Sonar.DECIMAL_FORMAT.format(failed))
          .replace("<total-success>", Sonar.DECIMAL_FORMAT.format(verified));
        return new SonarConfiguration.Webhook.Embed(
          config.getTitle(), config.getTitleUrl(), description, config.getR(), config.getG(), config.getB());
      });
    }

    if (genericWebhook != null) {
      final Map<String, Object> data = new LinkedHashMap<>();
      data.put("start-timestamp", startTimestamp);
      data.put("end-timestamp", endTimestamp);
      data.put("duration-millis", deltaInMillis);
      data.put("peak-cpu", attack.peakProcessCPUUsage);
      data.put("peak-memory-bytes", attack.peakProcessMemoryUsage);
      data.put("peak-bps", attack.peakJoinsPerSecond);
      data.put("peak-cps", attack.peakConnectionsPerSecond);
      data.put("total-blacklisted", blacklisted);
      data.put("total-failed", failed);
      data.put("total-success", verified);
      genericWebhook.post(GenericWebhook.ATTACK_END, data);
    }
  }

  @Nullable
  public AttackStatistics getCurrentAttack() {
    return this.currentAttack;
  }

  public int getAttackThreshold() {
    return this.attackThreshold;
  }

  public double getBaselineJoinsPerSecond() {
    return this.baselineJoinsPerSecond;
  }

  private AttackTracker() {
  }
}
