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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.antibot.subnet.SubnetRange;
import xyz.jonesdev.sonar.api.command.InvocationSource;
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand;
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandInfo;

import java.net.InetAddress;

@SubcommandInfo(
  name = "blacklist",
  arguments = {"add", "remove", "clear", "size"}
)
public final class BlacklistCommand extends Subcommand {

  @Override
  protected void execute(final @NotNull InvocationSource source, final String @NotNull [] args) {
    switch (args[1].toLowerCase()) {
      case "add": {
        if (args.length <= 2) {
          incorrectUsage(source, "blacklist add <IP address>");
          return;
        }

        final String rawAddress = toBlacklistKey(validateIP(source, args[2]));
        // Make sure the given IP address is valid
        if (rawAddress == null) return;

        if (Sonar.get0().getAntiBot().getBlacklist().asMap().containsKey(rawAddress)) {
          source.sendMessage(MiniMessage.miniMessage().deserialize(
            Sonar.get0().getConfig().getMessagesConfig().getString("commands.blacklist.ip-duplicate"),
            Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix()),
            Placeholder.unparsed("ip", rawAddress)));
          return;
        }

        Sonar.get0().getAntiBot().getBlacklist().put(rawAddress, 1337 /* arbitrarily high number */);
        Sonar.get0().getBlacklistController().save(rawAddress, 1337);
        source.sendMessage(MiniMessage.miniMessage().deserialize(
          Sonar.get0().getConfig().getMessagesConfig().getString("commands.blacklist.add"),
          Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix()),
          Placeholder.unparsed("ip", rawAddress)));
        break;
      }

      case "remove": {
        if (args.length <= 2) {
          incorrectUsage(source, "blacklist remove <IP address>");
          return;
        }

        final String rawAddress = toBlacklistKey(validateIP(source, args[2]));
        // Make sure the given IP address is valid
        if (rawAddress == null) return;

        if (!Sonar.get0().getAntiBot().getBlacklist().asMap().containsKey(rawAddress)) {
          source.sendMessage(MiniMessage.miniMessage().deserialize(
            Sonar.get0().getConfig().getMessagesConfig().getString("commands.blacklist.ip-not-found"),
            Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix())));
          return;
        }

        Sonar.get0().getAntiBot().getBlacklist().invalidate(rawAddress);
        Sonar.get0().getBlacklistController().remove(rawAddress);
        source.sendMessage(MiniMessage.miniMessage().deserialize(
          Sonar.get0().getConfig().getMessagesConfig().getString("commands.blacklist.remove"),
          Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix()),
          Placeholder.unparsed("ip", rawAddress)));
        break;
      }

      case "clear": {
        final long blacklistSize = Sonar.get0().getAntiBot().getBlacklist().estimatedSize();

        if (blacklistSize == 0) {
          source.sendMessage(MiniMessage.miniMessage().deserialize(
            Sonar.get0().getConfig().getMessagesConfig().getString("commands.blacklist.empty"),
            Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix())));
          return;
        }

        Sonar.get0().getAntiBot().getBlacklist().invalidateAll();
        Sonar.get0().getBlacklistController().clear();
        source.sendMessage(MiniMessage.miniMessage().deserialize(
          Sonar.get0().getConfig().getMessagesConfig().getString("commands.blacklist.clear"),
          Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix()),
          Placeholder.unparsed("amount", Sonar.DECIMAL_FORMAT.format(blacklistSize))));
        break;
      }

      case "size": {
        final long blacklistSize = Sonar.get0().getAntiBot().getBlacklist().estimatedSize();

        source.sendMessage(MiniMessage.miniMessage().deserialize(
          Sonar.get0().getConfig().getMessagesConfig().getString("commands.blacklist.size"),
          Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix()),
          Placeholder.unparsed("amount", Sonar.DECIMAL_FORMAT.format(blacklistSize))));
        break;
      }

      default: {
        incorrectUsage(source);
        break;
      }
    }
  }

  /**
   * Converts a validated IP address into the key used by the blacklist,
   * so manual entries are grouped by the same subnet as automatic ones.
   *
   * @param rawAddress The validated IP address or {@code null} if it was invalid
   * @return The blacklist key or {@code null} if the address was invalid
   */
  private static @Nullable String toBlacklistKey(final @Nullable String rawAddress) {
    if (rawAddress == null) {
      return null;
    }
    final InetAddress inetAddress = SubnetRange.parseLiteral(rawAddress);
    return inetAddress == null ? rawAddress : Sonar.get0().getAntiBot().getBlacklistKey(inetAddress);
  }
}
