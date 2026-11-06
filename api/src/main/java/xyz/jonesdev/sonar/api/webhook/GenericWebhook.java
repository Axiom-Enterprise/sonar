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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Posts attack notifications as plain JSON to any HTTP(S) endpoint
 * (e.g. automation tools, Telegram bridges, incident management).
 * <p>
 * Body layout: {@code {"event": "attack_start", "timestamp": 1730000000, "data": {...}}}
 */
public final class GenericWebhook {
  public static final String ATTACK_START = "attack_start";
  public static final String ATTACK_END = "attack_end";

  private static final Gson GSON = new GsonBuilder().create();

  private final String url;

  /**
   * @param url The HTTP(S) endpoint that receives the events
   */
  public GenericWebhook(final @NotNull String url) {
    this.url = url;
  }

  /**
   * Asynchronously posts an event to the configured URL
   *
   * @param event The name of the event (see {@link #ATTACK_START} and {@link #ATTACK_END})
   * @param data  Additional values of the event; serialized as a JSON object
   */
  public void post(final @NotNull String event, final @NotNull Map<String, Object> data) {
    WebhookHttpClient.postJson(url, () -> {
      final Map<String, Object> body = new LinkedHashMap<>();
      body.put("event", event);
      body.put("timestamp", System.currentTimeMillis() / 1000L);
      body.put("data", data);
      return GSON.toJson(body);
    }, code -> code >= 200 && code < 300, "generic");
  }
}
