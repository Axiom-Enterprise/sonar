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


package xyz.jonesdev.sonar.common.subcommand;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.command.InvocationSource;
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand;
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandInfo;
import xyz.jonesdev.sonar.api.database.model.AttackRecord;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SubcommandInfo(
  name = "history"
)
public final class HistoryCommand extends Subcommand {
  private static final int DEFAULT_AMOUNT = 5;

  @Override
  protected void execute(final @NotNull InvocationSource source, final String @NotNull [] args) {
    int amount = DEFAULT_AMOUNT;
    if (args.length > 1) {
      try {
        amount = Math.max(Integer.parseInt(args[1]), 1);
      } catch (NumberFormatException exception) {
        incorrectUsage(source, "history [amount]");
        return;
      }
    }

    final List<AttackRecord> records = Sonar.get0().getAttackHistoryController().getRecords();
    if (records.isEmpty()) {
      sendMessage(source, "empty");
      return;
    }

    final List<AttackRecord> shown = records.subList(0, Math.min(amount, records.size()));
    sendMessage(source, "header", Placeholder.unparsed("amount", String.valueOf(shown.size())));
    final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    for (final AttackRecord record : shown) {
      final long seconds = TimeUnit.MILLISECONDS.toSeconds(record.getDurationMillis());
      sendMessage(source, "entry",
        Placeholder.unparsed("date", dateFormat.format(record.getStart())),
        Placeholder.unparsed("duration", (seconds / 60) + "m " + (seconds % 60) + "s"),
        Placeholder.unparsed("peak-bps", Sonar.DECIMAL_FORMAT.format(record.getPeakJoinsPerSecond())),
        Placeholder.unparsed("peak-cps", Sonar.DECIMAL_FORMAT.format(record.getPeakConnectionsPerSecond())),
        Placeholder.unparsed("verified", Sonar.DECIMAL_FORMAT.format(record.getVerified())),
        Placeholder.unparsed("failed", Sonar.DECIMAL_FORMAT.format(record.getFailed())),
        Placeholder.unparsed("blacklisted", Sonar.DECIMAL_FORMAT.format(record.getBlacklisted())));
      if (!record.getTopSubnets().isEmpty()) {
        sendMessage(source, "subnets", Placeholder.unparsed("subnets", record.getTopSubnets()));
      }
    }
  }

  private static void sendMessage(final @NotNull InvocationSource source,
                                  final @NotNull String key,
                                  final TagResolver @NotNull ... placeholders) {
    source.sendMessage(MiniMessage.miniMessage().deserialize(
      Sonar.get0().getConfig().getMessagesConfig().getString("commands.history." + key),
      TagResolver.resolver(Placeholder.component("prefix", Sonar.get0().getConfig().getPrefix()),
        TagResolver.resolver(placeholders))));
  }
}
