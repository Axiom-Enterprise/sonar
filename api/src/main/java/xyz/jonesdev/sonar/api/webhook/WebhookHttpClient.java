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

package xyz.jonesdev.sonar.api.webhook;

import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

/**
 * Shared HTTP client for all webhooks; requests are sent one by one on a single background thread,
 * so a slow endpoint never blocks the Netty event loop or the attack tracker.
 */
final class WebhookHttpClient {
  private static final ExecutorService HTTP_REQUEST_SERVICE = Executors.newSingleThreadExecutor();

  private WebhookHttpClient() {
    // Utility class
  }

  /**
   * Asynchronously posts a JSON body to the given URL
   *
   * @param url                The target URL (HTTP or HTTPS)
   * @param body               Lazily serialized JSON body
   * @param isExpectedResponse Checks if the response code means success
   * @param name               Name of the webhook used in log messages
   */
  static void postJson(final @NotNull String url,
                final @NotNull Supplier<String> body,
                final @NotNull IntPredicate isExpectedResponse,
                final @NotNull String name) {
    HTTP_REQUEST_SERVICE.execute(() -> {
      try {
        final byte[] content = body.get().getBytes(StandardCharsets.UTF_8);
        final HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
        // Set connection timeouts
        urlConnection.setConnectTimeout(15000);
        urlConnection.setReadTimeout(15000);
        // Set necessary properties
        urlConnection.setRequestProperty("Content-Type", "application/json");
        // Use the byte length; the string length is wrong for non-ASCII characters
        urlConnection.setRequestProperty("Content-Length", String.valueOf(content.length));
        urlConnection.setRequestProperty("User-Agent", "Sonar");
        urlConnection.setDoOutput(true);
        urlConnection.setRequestMethod("POST");

        try (final OutputStream outputStream = urlConnection.getOutputStream()) {
          outputStream.write(content);
        }

        final int code = urlConnection.getResponseCode();
        if (!isExpectedResponse.test(code)) {
          Sonar.get0().getLogger().warn("Unexpected {} webhook response code {}", name, code);
        }
        urlConnection.disconnect();
      } catch (Exception exception) {
        Sonar.get0().getLogger().error("Could not send {} webhook: {}", name, exception);
      }
    });
  }
}
