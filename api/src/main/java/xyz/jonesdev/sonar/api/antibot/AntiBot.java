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
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.antibot.captcha.CaptchaGenerator;
import xyz.jonesdev.sonar.api.antibot.ratelimit.Ratelimiter;
import xyz.jonesdev.sonar.api.antibot.subnet.SubnetRange;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;

import java.net.InetAddress;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

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
  // Number of server list requests per address inside the current window
  private Cache<InetAddress, AtomicInteger> serverListRequests;
  // Number of new players per username pattern inside the current window
  private Cache<String, AtomicInteger> namePatterns;
  // Timestamp (in milliseconds) at which the lockdown ends; 0 if there is no lockdown
  private volatile long lockdownEnd;

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

  /**
   * Counts a server list request and checks if the address exceeded the configured limit.
   *
   * @param inetAddress The address that requested the server list
   * @return {@code true} if the request is allowed, {@code false} if the address sends too many requests
   */
  public boolean attemptServerListRequest(final @NotNull InetAddress inetAddress) {
    final int maxPings = Sonar.get0().getConfig().getServerListRateLimitMaxPings();
    if (maxPings <= 0 || serverListRequests == null || isAllowlisted(inetAddress)) {
      return true;
    }
    // The entry is never rewritten, so it expires a fixed amount of time after the first request
    final AtomicInteger requests = serverListRequests.get(inetAddress, __ -> new AtomicInteger());
    return requests.incrementAndGet() <= maxPings;
  }

  /**
   * Checks the hostname (server address) the client used to connect against the configured list.
   *
   * @param rawHostname The hostname from the handshake packet
   * @return {@code true} if the check is disabled or the hostname is allowed
   */
  public boolean isHostnameAllowed(final @Nullable String rawHostname) {
    final SonarConfiguration.Verification.Hostname config = Sonar.get0().getConfig().getVerification().getHostname();
    if (!config.isEnabled() || config.getAllowed().isEmpty()) {
      return true;
    }
    if (rawHostname == null) {
      return false;
    }
    final String hostname = normalizeHostname(rawHostname);
    for (final String allowed : config.getAllowed()) {
      if (allowed.startsWith("*.")
        ? hostname.endsWith(allowed.substring(1)) || hostname.equals(allowed.substring(2))
        : hostname.equals(allowed)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Removes everything that is not part of the actual hostname:
   * Forge/BungeeCord data after a null character, TCPShield data after "///" and the trailing dot of FQDNs.
   *
   * @param rawHostname The hostname from the handshake packet
   * @return The lower case hostname
   */
  public static @NotNull String normalizeHostname(final @NotNull String rawHostname) {
    String hostname = rawHostname;
    final int nullIndex = hostname.indexOf('\0');
    if (nullIndex != -1) {
      hostname = hostname.substring(0, nullIndex);
    }
    final int shieldIndex = hostname.indexOf("///");
    if (shieldIndex != -1) {
      hostname = hostname.substring(0, shieldIndex);
    }
    if (hostname.endsWith(".")) {
      hostname = hostname.substring(0, hostname.length() - 1);
    }
    return hostname.toLowerCase(Locale.ROOT);
  }

  /**
   * Counts a new player for the pattern of their username and checks if the pattern is used too often.
   * This only happens during an attack, so normal traffic is never affected.
   *
   * @param username The username of the new player
   * @return {@code true} if the player should be disconnected
   */
  public boolean isNamePatternBlocked(final @NotNull String username) {
    final SonarConfiguration.Verification.NamePattern config = Sonar.get0().getConfig().getVerification().getNamePattern();
    if (!config.isEnabled() || namePatterns == null || Sonar.get0().getAttackTracker().getCurrentAttack() == null) {
      return false;
    }
    final String pattern = toNamePattern(username);
    // Very short patterns (e.g. names that only consist of digits) would match too many real players
    if (pattern.length() < 3) {
      return false;
    }
    final AtomicInteger count = namePatterns.get(pattern, __ -> new AtomicInteger());
    return count.incrementAndGet() > config.getThreshold();
  }

  /**
   * Reduces a username to its pattern: digits are removed and letters are converted to lower case.
   * For example, {@code Bot_123} and {@code bot_9} both become {@code bot_}.
   *
   * @param username The username of the player
   * @return The pattern of the username
   */
  public static @NotNull String toNamePattern(final @NotNull String username) {
    final StringBuilder pattern = new StringBuilder(username.length());
    for (int i = 0; i < username.length(); i++) {
      final char character = username.charAt(i);
      if (character < '0' || character > '9') {
        pattern.append(Character.toLowerCase(character));
      }
    }
    return pattern.toString();
  }

  /**
   * Starts (or extends) a lockdown: players that have never been verified cannot join until it ends.
   *
   * @param durationMillis How long the lockdown lasts (in milliseconds)
   */
  public void enableLockdown(final long durationMillis) {
    lockdownEnd = System.currentTimeMillis() + durationMillis;
  }

  /**
   * Ends the lockdown immediately.
   */
  public void disableLockdown() {
    lockdownEnd = 0L;
  }

  /**
   * @return {@code true} if a lockdown is active; it automatically ends once its duration has passed
   */
  public boolean isLockdownActive() {
    return getLockdownRemaining() > 0L;
  }

  /**
   * @return The remaining duration of the lockdown in milliseconds, or 0 if there is no lockdown
   */
  public long getLockdownRemaining() {
    return Math.max(lockdownEnd - System.currentTimeMillis(), 0L);
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

  public Cache<InetAddress, AtomicInteger> getServerListRequests() {
    return this.serverListRequests;
  }

  public Cache<String, AtomicInteger> getNamePatterns() {
    return this.namePatterns;
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

  public void setServerListRequests(final Cache<InetAddress, AtomicInteger> serverListRequests) {
    this.serverListRequests = serverListRequests;
  }

  public void setNamePatterns(final Cache<String, AtomicInteger> namePatterns) {
    this.namePatterns = namePatterns;
  }
}
