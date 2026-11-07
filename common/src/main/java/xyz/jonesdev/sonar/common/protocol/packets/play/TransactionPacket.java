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

package xyz.jonesdev.sonar.common.protocol.packets.play;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.antibot.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.protocol.SonarPacket;

public final class TransactionPacket implements SonarPacket {
  private int windowId;
  private int transactionId;
  private boolean accepted;

  @Override
  public void decode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_17)) {
      windowId = byteBuf.readByte();
      transactionId = byteBuf.readShort();
      accepted = byteBuf.readBoolean();
    } else {
      transactionId = byteBuf.readInt();
      // Always set accepted to true since 1.17 or higher don't use
      // transactions for inventory confirmation anymore.
      accepted = true;
    }
  }

  @Override
  public void encode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_17)) {
      byteBuf.writeByte(windowId);
      byteBuf.writeShort((short) transactionId);
      byteBuf.writeBoolean(accepted);
    } else {
      byteBuf.writeInt(transactionId);
    }
  }

  public int getWindowId() {
    return this.windowId;
  }

  public int getTransactionId() {
    return this.transactionId;
  }

  public boolean isAccepted() {
    return this.accepted;
  }

  @java.lang.Override
  public java.lang.String toString() {
    return "TransactionPacket(windowId=" + this.getWindowId() + ", transactionId=" + this.getTransactionId() + ", accepted=" + this.isAccepted() + ")";
  }

  public TransactionPacket() {
  }

  public TransactionPacket(final int windowId, final int transactionId, final boolean accepted) {
    this.windowId = windowId;
    this.transactionId = transactionId;
    this.accepted = accepted;
  }
}
