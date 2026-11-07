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
import xyz.jonesdev.sonar.api.antibot.subnet.SubnetRange;
import xyz.jonesdev.sonar.api.command.InvocationSource;
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand;
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandInfo;

import java.util.List;
import java.util.stream.Collectors;

@SubcommandInfo(
  name = "allowlist",
  arguments = {"add", "remove", "list"}
)
public final class AllowlistCommand extends Subcommand {

  @Override
  protected void execute(final @NotNull InvocationSource source, final String @NotNull [] args) {
    switch (args[1].toLowerCase()) {
      case "add": {
        if (args.length <= 2) {
          incorrectUsage(source, "allowlist add <IP address or CIDR range>");
          return;
        }
        final SubnetRange range = SubnetRange.parse(args[2]);
        if (range == null) {
          sendMessage(source, "invalid");
          return;
        }
        // Saving the configuration changes the allowlist immediately, so no reload is needed
        if (!updateAllowlist(true, args[2])) {
          sendMessage(source, "duplicate");
          return;
        }
        sendMessage(source, "add", Placeholder.unparsed("entry", range.toString()));
        break;
      }

      case "remove": {
        if (args.length <= 2) {
          incorrectUsage(source, "allowlist remove <IP address or CIDR range>");
          return;
        }
        final SubnetRange range = SubnetRange.parse(args[2]);
        if (range == null) {
          sendMessage(source, "invalid");
          return;
        }
        if (!updateAllowlist(false, args[2])) {
          sendMessage(source, "not-found");
          return;
        }
        sendMessage(source, "remove", Placeholder.unparsed("entry", range.toString()));
        break;
      }

      case "list": {
        final List<SubnetRange> allowlist = Sonar.get0().getConfig().getAllowlist();
        if (allowlist.isEmpty()) {
          sendMessage(source, "empty");
          return;
        }
        final String entries = allowlist.stream()
          .map(SubnetRange::toString)
          .collect(Collectors.joining(", "));
        sendMessage(source, "list",
          Placeholder.unparsed("amount", Sonar.DECIMAL_FORMAT.format(allowlist.size())),
          Placeholder.unparsed("entries", entries));
        break;
      }

      default: {
        incorrectUsage(source);
        break;
      }
    }
  }

  /**
   * Adds or removes the entry and saves the configuration file.
   *
   * @return {@code false} if nothing changed (duplicate or missing entry)
   */
  private static boolean updateAllowlist(final boolean add, final @NotNull String raw) {
    try {
      return add
        ? Sonar.get0().getConfig().addToAllowlist(raw)
        : Sonar.get0().getConfig().removeFromAllowlist(raw);
    } catch (Exception exception) {
      // The change could not be written to config.yml; tell the user instead of failing silently
      Sonar.get0().getLogger().error("Could not save the allowlist: {}", exception);
      throw new IllegalStateException("Could not save the allowlist", exception);
    }
  }

  private static void sendMessage(final @NotNull InvocationSource source,
                                  final @NotNull String key,
                                  final TagResolver @NotNull ... placeholders) {
    source.sendMessage(MiniMessage.miniMessage().deserialize(
      Sonar.get0().getConfig().getMessagesConfig().getString("commands.allowlist." + key),
      TagResolver.resolver(Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix()),
        TagResolver.resolver(placeholders))));
  }
}
