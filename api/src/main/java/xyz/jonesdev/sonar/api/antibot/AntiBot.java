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

package xyz.jonesdev.sonar.api.antibot;

import com.github.benmanes.caffeine.cache.Cache;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.antibot.captcha.CaptchaGenerator;
import xyz.jonesdev.sonar.api.antibot.ratelimit.Ratelimiter;
import xyz.jonesdev.sonar.api.antibot.subnet.SubnetRange;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;

import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class AntiBot {
  public static final AntiBot INSTANCE = new AntiBot();

  private final ConcurrentMap<InetAddress, Integer> online = new ConcurrentHashMap<>(128);
  private final ConcurrentMap<InetAddress, Boolean> connected = new ConcurrentHashMap<>(256);
  private final LoginQueue queue = new LoginQueue();
  private Cache<String, Integer> blacklist;
  private long blacklistTime;
  private CaptchaGenerator captchaGenerator;
  private Ratelimiter<InetAddress> ratelimiter;
  private Cache<InetAddress, Boolean> serverListPings;
  private long serverListPingMaxAge;

  /**
   * Builds the blacklist key of an address by grouping it into its configured subnet.
   * This prevents attackers from bypassing the blacklist by rotating addresses inside the same network
   * (e.g. a single IPv6 /64 contains billions of addresses).
   *
   * @param inetAddress The address of the player
   * @return The key used inside the blacklist cache
   */
  public @NotNull String getBlacklistKey(final @NotNull InetAddress inetAddress) {
    final SonarConfiguration.Verification verification = Sonar.get0().getConfig().getVerification();
    final int prefixLength = inetAddress.getAddress().length == 4
      ? verification.getBlacklistIpv4Prefix()
      : verification.getBlacklistIpv6Prefix();
    return SubnetRange.toSubnetKey(inetAddress, prefixLength);
  }

  /**
   * @param inetAddress The address of the player
   * @return {@code true} if the address is inside one of the configured allowlist ranges
   */
  public boolean isAllowlisted(final @NotNull InetAddress inetAddress) {
    for (final SubnetRange range : Sonar.get0().getConfig().getAllowlist()) {
      if (range.contains(inetAddress)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Remembers that the given address has requested the server list (status handshake).
   *
   * @param inetAddress The address that pinged the server
   */
  public void markServerListPing(final @NotNull InetAddress inetAddress) {
    if (serverListPings != null) {
      serverListPings.put(inetAddress, Boolean.TRUE);
    }
  }

  /**
   * @param inetAddress The address of the player
   * @return {@code true} if the address must have pinged the server list before joining, but did not
   */
  public boolean isMissingServerListPing(final @NotNull InetAddress inetAddress) {
    return serverListPings != null
      && shouldPerform(Sonar.get0().getConfig().getVerification().getServerListPing().getTiming())
      && serverListPings.getIfPresent(inetAddress) == null;
  }

  public boolean shouldVerifyNewPlayers() {
    return shouldPerform(Sonar.get0().getConfig().getVerification().getTiming());
  }

  public boolean shouldPerformCaptcha() {
    return shouldPerform(Sonar.get0().getConfig().getVerification().getMap().getTiming());
  }

  public static boolean shouldPerform(final SonarConfiguration.Verification.Timing timing) {
    return timing == SonarConfiguration.Verification.Timing.ALWAYS
      || (timing == SonarConfiguration.Verification.Timing.DURING_ATTACK
      && Sonar.get0().getAttackTracker().getCurrentAttack() != null);
  }

  public ConcurrentMap<InetAddress, Integer> getOnline() {
    return this.online;
  }

  public ConcurrentMap<InetAddress, Boolean> getConnected() {
    return this.connected;
  }

  public LoginQueue getQueue() {
    return this.queue;
  }

  public Cache<String, Integer> getBlacklist() {
    return this.blacklist;
  }

  public long getBlacklistTime() {
    return this.blacklistTime;
  }

  public CaptchaGenerator getCaptchaGenerator() {
    return this.captchaGenerator;
  }

  public Ratelimiter<InetAddress> getRatelimiter() {
    return this.ratelimiter;
  }

  public Cache<InetAddress, Boolean> getServerListPings() {
    return this.serverListPings;
  }

  public long getServerListPingMaxAge() {
    return this.serverListPingMaxAge;
  }

  private AntiBot() {
  }

  public void setBlacklist(final Cache<String, Integer> blacklist) {
    this.blacklist = blacklist;
  }

  public void setBlacklistTime(final long blacklistTime) {
    this.blacklistTime = blacklistTime;
  }

  public void setCaptchaGenerator(final CaptchaGenerator captchaGenerator) {
    this.captchaGenerator = captchaGenerator;
  }

  public void setRatelimiter(final Ratelimiter<InetAddress> ratelimiter) {
    this.ratelimiter = ratelimiter;
  }

  public void setServerListPings(final Cache<InetAddress, Boolean> serverListPings) {
    this.serverListPings = serverListPings;
  }

  public void setServerListPingMaxAge(final long serverListPingMaxAge) {
    this.serverListPingMaxAge = serverListPingMaxAge;
  }
}
