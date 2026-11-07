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

package xyz.jonesdev.sonar.common.metrics;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;
import xyz.jonesdev.sonar.api.statistics.SonarStatistics;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Small HTTP endpoint that exposes Sonar's statistics in the Prometheus text format,
 * so they can be scraped by Prometheus and displayed in Grafana.
 * It uses the HTTP server built into the JDK, so no additional library is needed.
 */
public final class SonarMetricsServer {
  private static final String CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";

  private @Nullable HttpServer server;
  private @Nullable ExecutorService executor;
  // The configuration the server is currently running with, used to avoid needless restarts
  private @Nullable String runningConfig;

  /**
   * Starts, restarts or stops the endpoint depending on the current configuration.
   * Errors (e.g. a port that is already in use) are logged instead of preventing Sonar from loading.
   */
  public synchronized void apply(final @NotNull SonarConfiguration.Metrics config) {
    final String newConfig = config.isEnabled()
      ? config.getHost() + ":" + config.getPort() + ":" + config.getToken() : null;
    if (newConfig != null && newConfig.equals(runningConfig)) {
      return;
    }
    stop();
    if (newConfig == null) {
      return;
    }
    try {
      final HttpServer httpServer = HttpServer.create(new InetSocketAddress(config.getHost(), config.getPort()), 0);
      final byte[] token = config.getToken().getBytes(StandardCharsets.UTF_8);
      httpServer.createContext("/metrics", exchange -> handle(exchange, token));
      executor = Executors.newSingleThreadExecutor(runnable -> {
        final Thread thread = Executors.defaultThreadFactory().newThread(runnable);
        thread.setName("sonar-metrics-thread");
        thread.setDaemon(true);
        return thread;
      });
      httpServer.setExecutor(executor);
      httpServer.start();
      server = httpServer;
      runningConfig = newConfig;
      Sonar.get0().getLogger().info("Metrics endpoint listening on http://{}:{}/metrics",
        config.getHost(), config.getPort());
    } catch (Exception exception) {
      Sonar.get0().getLogger().error("Could not start the metrics endpoint: {}", exception);
      stop();
    }
  }

  /**
   * Stops the endpoint if it is running.
   */
  public synchronized void stop() {
    if (server != null) {
      server.stop(0);
      server = null;
    }
    if (executor != null) {
      executor.shutdownNow();
      executor = null;
    }
    runningConfig = null;
  }

  private static void handle(final @NotNull HttpExchange exchange, final byte @NotNull [] token) throws IOException {
    try {
      if (!"GET".equals(exchange.getRequestMethod())) {
        exchange.sendResponseHeaders(405, -1);
        return;
      }
      if (token.length > 0 && !isAuthorized(exchange, token)) {
        exchange.sendResponseHeaders(401, -1);
        return;
      }
      final byte[] body = collect().getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE);
      exchange.sendResponseHeaders(200, body.length);
      try (final OutputStream outputStream = exchange.getResponseBody()) {
        outputStream.write(body);
      }
    } finally {
      exchange.close();
    }
  }

  private static boolean isAuthorized(final @NotNull HttpExchange exchange, final byte @NotNull [] token) {
    final String header = exchange.getRequestHeaders().getFirst("Authorization");
    if (header == null || !header.startsWith("Bearer ")) {
      return false;
    }
    // Constant time comparison, so the token cannot be guessed by measuring the response time
    return MessageDigest.isEqual(header.substring(7).getBytes(StandardCharsets.UTF_8), token);
  }

  /**
   * @return All metrics in the Prometheus text exposition format
   */
  static @NotNull String collect() {
    final SonarStatistics statistics = Sonar.get0().getStatistics();
    final StringBuilder builder = new StringBuilder(2048);
    gauge(builder, "sonar_connections_per_second", "Connections per second", statistics.getConnectionsPerSecond());
    gauge(builder, "sonar_logins_per_second", "Login attempts per second", statistics.getLoginsPerSecond());
    counter(builder, "sonar_players_joined_total", "Players that joined since the start", statistics.getTotalPlayersJoined());
    counter(builder, "sonar_verifications_attempted_total", "Attempted verifications", statistics.getTotalAttemptedVerifications());
    counter(builder, "sonar_verifications_successful_total", "Successful verifications", statistics.getTotalSuccessfulVerifications());
    counter(builder, "sonar_verifications_failed_total", "Failed verifications", statistics.getTotalFailedVerifications());
    gauge(builder, "sonar_verifying_players", "Players currently being verified", Sonar.get0().getAntiBot().getConnected().size());
    gauge(builder, "sonar_queued_players", "Players currently in the login queue", Sonar.get0().getAntiBot().getQueue().getPlayers().size());
    gauge(builder, "sonar_verified_players", "Verified players", Sonar.get0().getVerifiedPlayerController().getCache().size());
    gauge(builder, "sonar_blacklist_size", "Blacklisted addresses or subnets", statistics.getCurrentBlacklistSize());
    gauge(builder, "sonar_under_attack", "1 if an attack is in progress", Sonar.get0().getAttackTracker().getCurrentAttack() != null ? 1 : 0);
    gauge(builder, "sonar_attack_threshold", "Players per second needed to detect an attack", Sonar.get0().getAttackTracker().getEffectiveMinPlayers());
    gauge(builder, "sonar_baseline_logins_per_second", "Learned normal amount of logins per second", Sonar.get0().getAttackTracker().getBaselineJoinsPerSecond());
    gauge(builder, "sonar_lockdown_active", "1 if the lockdown is active", Sonar.get0().getAntiBot().isLockdownActive() ? 1 : 0);
    gauge(builder, "sonar_incoming_bytes_per_second", "Incoming traffic per second", statistics.getCurrentIncomingBandwidth());
    gauge(builder, "sonar_outgoing_bytes_per_second", "Outgoing traffic per second", statistics.getCurrentOutgoingBandwidth());
    counter(builder, "sonar_incoming_bytes_total", "Total incoming traffic", statistics.getTotalIncomingBandwidth());
    counter(builder, "sonar_outgoing_bytes_total", "Total outgoing traffic", statistics.getTotalOutgoingBandwidth());
    return builder.toString();
  }

  private static void gauge(final StringBuilder builder, final String name, final String help, final double value) {
    metric(builder, name, help, "gauge", value);
  }

  private static void counter(final StringBuilder builder, final String name, final String help, final double value) {
    metric(builder, name, help, "counter", value);
  }

  private static void metric(final @NotNull StringBuilder builder, final String name, final String help,
                             final String type, final double value) {
    builder.append("# HELP ").append(name).append(' ').append(help).append('\n');
    builder.append("# TYPE ").append(name).append(' ').append(type).append('\n');
    // Whole numbers are written without decimals; Locale.ROOT guarantees a dot as decimal separator
    builder.append(name).append(' ')
      .append(value == Math.rint(value) ? Long.toString((long) value) : String.format(Locale.ROOT, "%.3f", value))
      .append('\n');
  }
}
