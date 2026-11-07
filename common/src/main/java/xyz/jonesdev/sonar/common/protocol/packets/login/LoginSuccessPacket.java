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

package xyz.jonesdev.sonar.common.protocol.packets.login;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.antibot.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.protocol.SonarPacket;
import xyz.jonesdev.sonar.common.util.FastUuidSansHyphens;
import xyz.jonesdev.sonar.common.util.ProtocolUtil;

import java.util.UUID;

public final class LoginSuccessPacket implements SonarPacket {
  private UUID uuid;
  private String username;
  private boolean strictErrorHandling;
  private UUID sessionId;

  @Override
  public void encode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_16)) {
      ProtocolUtil.writeUUID(byteBuf, uuid);
    } else if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_7_6)) {
      ProtocolUtil.writeString(byteBuf, uuid.toString());
    } else {
      ProtocolUtil.writeString(byteBuf, FastUuidSansHyphens.toString(uuid));
    }

    ProtocolUtil.writeString(byteBuf, username);

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_19)) {
      // We don't need to send any properties to the client
      ProtocolUtil.writeVarInt(byteBuf, 0);
    }

    if (protocolVersion.equals(ProtocolVersion.MINECRAFT_1_20_5)
      || protocolVersion.equals(ProtocolVersion.MINECRAFT_1_21)) {
      // Whether the client should disconnect on its own if it receives invalid data from the server
      byteBuf.writeBoolean(strictErrorHandling);
    }

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_26_2)) {
      ProtocolUtil.writeUUID(byteBuf, sessionId);
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }

  public UUID getUuid() {
    return this.uuid;
  }

  public String getUsername() {
    return this.username;
  }

  public boolean isStrictErrorHandling() {
    return this.strictErrorHandling;
  }

  public UUID getSessionId() {
    return this.sessionId;
  }

  @java.lang.Override
  public java.lang.String toString() {
    return "LoginSuccessPacket(uuid=" + this.getUuid() + ", username=" + this.getUsername() + ", strictErrorHandling=" + this.isStrictErrorHandling() + ", sessionId=" + this.getSessionId() + ")";
  }

  public LoginSuccessPacket() {
  }

  public LoginSuccessPacket(final UUID uuid, final String username, final boolean strictErrorHandling, final UUID sessionId) {
    this.uuid = uuid;
    this.username = username;
    this.strictErrorHandling = strictErrorHandling;
    this.sessionId = sessionId;
  }
}
