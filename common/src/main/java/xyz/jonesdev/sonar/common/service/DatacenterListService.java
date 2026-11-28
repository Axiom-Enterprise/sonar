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


package xyz.jonesdev.sonar.common.service;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.antibot.subnet.SubnetRange;
import xyz.jonesdev.sonar.api.antibot.subnet.SubnetRangeSet;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Loads the datacenter and VPN networks from the configured files and URLs into {@code AntiBot}.
 * <p>
 * Every line contains one IP address or CIDR range; everything after {@code #}, {@code ;} or a space is ignored,
 * so common public lists (e.g. Spamhaus DROP) can be used without changes.
 * Everything runs on a background thread; the URLs are downloaded again after the configured interval.
 * If a download fails, the networks of the last successful download are kept.
 */
public final class DatacenterListService {
  // Lists larger than this are most likely not a list of networks
  private static final long MAX_DOWNLOAD_BYTES = 64L * 1024L * 1024L;
  private static final ScheduledExecutorService SERVICE = Executors.newSingleThreadScheduledExecutor(runnable -> {
    final Thread thread = Executors.defaultThreadFactory().newThread(runnable);
    thread.setName("sonar-datacenter-thread");
    thread.setDaemon(true);
    return thread;
  });
  // Networks of the last successful download per URL
  private static final Map<String, List<SubnetRange>> DOWNLOADED = new ConcurrentHashMap<>();
  private static @Nullable ScheduledFuture<?> refreshTask;

  /**
   * (Re-)loads all lists using the current configuration; called on every reload
   */
  public static synchronized void apply() {
    if (refreshTask != null) {
      refreshTask.cancel(false);
      refreshTask = null;
    }
    final SonarConfiguration.Verification.Datacenter config = Sonar.get0().getConfig().getVerification().getDatacenter();
    DOWNLOADED.keySet().retainAll(config.getUrls());
    if (config.getFiles().isEmpty() && config.getUrls().isEmpty()) {
      Sonar.get0().getAntiBot().setDatacenterRanges(SubnetRangeSet.EMPTY);
      return;
    }
    final File pluginFolder = Sonar.get0().getConfig().getPluginFolder();
    refreshTask = SERVICE.scheduleWithFixedDelay(() -> load(pluginFolder, config),
      0L, config.getRefreshInterval(), TimeUnit.HOURS);
  }

  public static void stop() {
    SERVICE.shutdownNow();
  }

  private static void load(final @NotNull File pluginFolder,
                           final SonarConfiguration.Verification.Datacenter config) {
    final List<SubnetRange> ranges = new ArrayList<>();
    for (final String path : config.getFiles()) {
      final File file = new File(pluginFolder, path);
      try (final InputStream inputStream = Files.newInputStream(file.toPath())) {
        ranges.addAll(parse(inputStream));
      } catch (IOException exception) {
        Sonar.get0().getLogger().error("Could not read datacenter list {}: {}", file.getAbsolutePath(), exception);
      }
    }
    for (final String url : config.getUrls()) {
      try {
        DOWNLOADED.put(url, download(url));
      } catch (IOException exception) {
        Sonar.get0().getLogger().error("Could not download datacenter list {}: {}", url, exception);
      }
      ranges.addAll(DOWNLOADED.getOrDefault(url, List.of()));
    }
    final SubnetRangeSet set = SubnetRangeSet.of(ranges);
    Sonar.get0().getAntiBot().setDatacenterRanges(set);
    Sonar.get0().getLogger().info("Loaded {} datacenter networks ({} after merging).", ranges.size(), set.size());
  }

  private static @NotNull List<SubnetRange> download(final @NotNull String url) throws IOException {
    final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    connection.setConnectTimeout(15000);
    connection.setReadTimeout(30000);
    connection.setRequestProperty("User-Agent", "Sonar");
    try {
      final int responseCode = connection.getResponseCode();
      if (responseCode != HttpURLConnection.HTTP_OK) {
        throw new IOException("Unexpected response code " + responseCode);
      }
      if (connection.getContentLengthLong() > MAX_DOWNLOAD_BYTES) {
        throw new IOException("The list is larger than " + MAX_DOWNLOAD_BYTES + " bytes");
      }
      try (final InputStream inputStream = connection.getInputStream()) {
        return parse(inputStream);
      }
    } finally {
      connection.disconnect();
    }
  }

  private static @NotNull List<SubnetRange> parse(final @NotNull InputStream inputStream) throws IOException {
    final List<SubnetRange> ranges = new ArrayList<>();
    long bytes = 0L;
    try (final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        // Also limit lists without a content length (e.g. chunked responses)
        bytes += line.length() + 1;
        if (bytes > MAX_DOWNLOAD_BYTES) {
          throw new IOException("The list is larger than " + MAX_DOWNLOAD_BYTES + " bytes");
        }
        final String entry = stripComment(line);
        if (entry.isEmpty()) {
          continue;
        }
        final SubnetRange range = SubnetRange.parse(entry);
        if (range != null) {
          ranges.add(range);
        }
      }
    }
    return ranges;
  }

  private static @NotNull String stripComment(final @NotNull String line) {
    final String trimmed = line.trim();
    int end = trimmed.length();
    for (int i = 0; i < trimmed.length(); i++) {
      final char character = trimmed.charAt(i);
      if (character == '#' || character == ';' || Character.isWhitespace(character)) {
        end = i;
        break;
      }
    }
    return trimmed.substring(0, end);
  }

  private DatacenterListService() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }
}
