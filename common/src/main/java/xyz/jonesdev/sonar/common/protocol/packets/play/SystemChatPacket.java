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
import xyz.jonesdev.sonar.common.util.ComponentHolder;
import xyz.jonesdev.sonar.common.util.ProtocolUtil;
import xyz.jonesdev.sonar.common.util.exception.QuietDecoderException;

import java.util.UUID;

public final class SystemChatPacket implements SonarPacket {
  private static final int DIV_FLOOR = -Math.floorDiv(-20, 8);

  private ComponentHolder componentHolder;
  private String message;

  public SystemChatPacket(final @NotNull ComponentHolder componentHolder) {
    this.componentHolder = componentHolder;
  }

  @Override
  public void encode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    // Serialized message
    componentHolder.write(byteBuf, protocolVersion, false);

    // Type
    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_19_1)) {
      byteBuf.writeBoolean(false); // it's not the GAME_INFO type
    } else if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_19)) {
      ProtocolUtil.writeVarInt(byteBuf, 1); // system chat
    } else if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_8)) {
      byteBuf.writeByte(1); // system chat
    }

    // Sender
    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_16)
      && protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_19)) {
      ProtocolUtil.writeUUID(byteBuf, UUID.randomUUID());
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    message = ProtocolUtil.readString(byteBuf, 256);

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_19)) {
      byteBuf.readLong(); // timestamp

      if (protocolVersion.lessThanOrEquals(ProtocolVersion.MINECRAFT_1_19_1)) {
        final long saltLong = byteBuf.readLong();
        final byte[] signatureBytes = ProtocolUtil.readByteArray(byteBuf);
        boolean unsigned = false;

        if (saltLong != 0L && signatureBytes.length > 0) {
          // No need to store the valid signature
        } else if ((protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_19_1)
          || saltLong == 0L) && signatureBytes.length == 0) {
          unsigned = true;
        } else {
          throw QuietDecoderException.INSTANCE;
        }

        final boolean signedPreview = byteBuf.readBoolean();
        if (signedPreview && unsigned) {
          throw QuietDecoderException.INSTANCE;
        }

        if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_19_1)) {
          final int size = ProtocolUtil.readVarInt(byteBuf);
          if (size < 0 || size > 5) {
            throw QuietDecoderException.INSTANCE;
          }

          for (int i = 0; i < size; i++) {
            ProtocolUtil.readUUID(byteBuf);
            ProtocolUtil.readByteArray(byteBuf);
          }

          if (byteBuf.readBoolean()) {
            ProtocolUtil.readUUID(byteBuf);
            ProtocolUtil.readByteArray(byteBuf);
          }
        }
      } else {
        byteBuf.readLong(); // salt
        final boolean signed = byteBuf.readBoolean();

        if (signed) {
          final byte[] sign = new byte[256];
          byteBuf.readBytes(sign);
        }

        ProtocolUtil.readVarInt(byteBuf);
        final byte[] bytes = new byte[DIV_FLOOR];
        byteBuf.readBytes(bytes);

        if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_21_5)) {
          byteBuf.readByte(); // Check sum
        }
      }
    }
  }

  public ComponentHolder getComponentHolder() {
    return this.componentHolder;
  }

  public String getMessage() {
    return this.message;
  }

  public void setComponentHolder(final ComponentHolder componentHolder) {
    this.componentHolder = componentHolder;
  }

  public void setMessage(final String message) {
    this.message = message;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object o) {
    if (o == this) return true;
    if (!(o instanceof SystemChatPacket)) return false;
    final SystemChatPacket other = (SystemChatPacket) o;
    final java.lang.Object this$componentHolder = this.getComponentHolder();
    final java.lang.Object other$componentHolder = other.getComponentHolder();
    if (this$componentHolder == null ? other$componentHolder != null : !this$componentHolder.equals(other$componentHolder)) return false;
    final java.lang.Object this$message = this.getMessage();
    final java.lang.Object other$message = other.getMessage();
    if (this$message == null ? other$message != null : !this$message.equals(other$message)) return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    final int PRIME = 59;
    int result = 1;
    final java.lang.Object $componentHolder = this.getComponentHolder();
    result = result * PRIME + ($componentHolder == null ? 43 : $componentHolder.hashCode());
    final java.lang.Object $message = this.getMessage();
    result = result * PRIME + ($message == null ? 43 : $message.hashCode());
    return result;
  }

  @java.lang.Override
  public java.lang.String toString() {
    return "SystemChatPacket(componentHolder=" + this.getComponentHolder() + ", message=" + this.getMessage() + ")";
  }

  public SystemChatPacket() {
  }

  public SystemChatPacket(final ComponentHolder componentHolder, final String message) {
    this.componentHolder = componentHolder;
    this.message = message;
  }
}
