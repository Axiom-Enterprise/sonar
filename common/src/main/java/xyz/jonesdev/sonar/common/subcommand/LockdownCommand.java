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

package xyz.jonesdev.sonar.common.subcommand;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.command.InvocationSource;
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand;
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandInfo;

import java.util.concurrent.TimeUnit;

@SubcommandInfo(
  name = "lockdown",
  arguments = {"on", "off", "status"}
)
public final class LockdownCommand extends Subcommand {
  // One week; longer lockdowns are most likely a typo
  private static final int MAX_DURATION_MINUTES = 10080;

  @Override
  protected void execute(final @NotNull InvocationSource source, final String @NotNull [] args) {
    switch (args[1].toLowerCase()) {
      case "on": {
        int minutes = Sonar.get0().getConfig().getLockdownDefaultDuration();
        if (args.length > 2) {
          try {
            minutes = Integer.parseInt(args[2]);
          } catch (NumberFormatException exception) {
            minutes = -1;
          }
          if (minutes < 1 || minutes > MAX_DURATION_MINUTES) {
            sendMessage(source, "invalid-duration");
            return;
          }
        }

        Sonar.get0().getAntiBot().enableLockdown(TimeUnit.MINUTES.toMillis(minutes));
        Sonar.get0().getLogger().warn("Lockdown enabled for {} minute(s).", minutes);
        sendMessage(source, "enabled", Placeholder.unparsed("minutes", String.valueOf(minutes)));
        break;
      }

      case "off": {
        Sonar.get0().getAntiBot().disableLockdown();
        Sonar.get0().getLogger().info("Lockdown disabled.");
        sendMessage(source, "disabled");
        break;
      }

      case "status": {
        final long remaining = Sonar.get0().getAntiBot().getLockdownRemaining();
        if (remaining <= 0L) {
          sendMessage(source, "status-inactive");
          return;
        }
        // Round up, so the last minute is not shown as 0 minutes
        final long minutes = (remaining + TimeUnit.MINUTES.toMillis(1) - 1) / TimeUnit.MINUTES.toMillis(1);
        sendMessage(source, "status-active", Placeholder.unparsed("minutes", String.valueOf(minutes)));
        break;
      }

      default: {
        incorrectUsage(source);
        break;
      }
    }
  }

  private static void sendMessage(final @NotNull InvocationSource source,
                                  final @NotNull String key,
                                  final TagResolver @NotNull ... placeholders) {
    source.sendMessage(MiniMessage.miniMessage().deserialize(
      Sonar.get0().getConfig().getMessagesConfig().getString("commands.lockdown." + key),
      TagResolver.resolver(Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix()),
        TagResolver.resolver(placeholders))));
  }
}
