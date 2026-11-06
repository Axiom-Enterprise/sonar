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

package xyz.jonesdev.sonar.api.webhook;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;

@RequiredArgsConstructor
public final class DiscordWebhook {
  private final String url;

  private static final Gson GSON = new GsonBuilder().create();

  /**
   * Asynchronously posts a Discord webhook via HTTPS
   */
  public void post(final Supplier<SonarConfiguration.Webhook.Embed> embed) {
    // Discord replies with HTTP_NO_CONTENT (204) if the request is successful
    WebhookHttpClient.postJson(url, () -> prepareSerializedContent(embed.get()),
      code -> code == HTTP_NO_CONTENT, "Discord");
  }

  private static String prepareSerializedContent(final SonarConfiguration.Webhook.@NotNull Embed embed) {
    final String content = Sonar.get0().getConfig().getWebhook().getContent();
    final String username = Sonar.get0().getConfig().getWebhook().getUsername();
    final String avatarUrl = Sonar.get0().getConfig().getWebhook().getAvatarUrl();

    int rgb = embed.getR();
    rgb = (rgb << 8) + embed.getG();
    rgb = (rgb << 8) + embed.getB();

    final Webhook.EmbedMessage embedMessage = new Webhook.EmbedMessage(
      embed.getTitle(), embed.getDescription(), embed.getTitleUrl(), rgb, new Webhook.Field[0],
      new Webhook.Footer(Sonar.get0().getConfig().getWebhook().getFooter().getText(),
        Sonar.get0().getConfig().getWebhook().getFooter().getIconUrl()));
    final List<Webhook.EmbedMessage> embeds = Collections.singletonList(embedMessage);

    final Webhook webhook = new Webhook(content, username, avatarUrl, false, embeds);
    return GSON.toJson(webhook);
  }

  @SuppressWarnings("unused")
  @RequiredArgsConstructor
  static final class Webhook {
    private final String content, username, avatar_url;
    private final boolean tts;
    private final List<EmbedMessage> embeds;

    @RequiredArgsConstructor
    static final class EmbedMessage {
      private final String title;
      private final String description;
      private final String url;
      private final int color;
      private final Field[] fields;
      private final Footer footer;
    }

    @RequiredArgsConstructor
    static final class Field {
      private final String name;
      private final String value;
      private final boolean inline;
    }

    @RequiredArgsConstructor
    static final class Footer {
      private final String text;
      private final String icon_url;
    }
  }
}
