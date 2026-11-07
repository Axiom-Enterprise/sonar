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
import xyz.jonesdev.sonar.common.util.ProtocolUtil;

public final class RemoveEntitiesPacket implements SonarPacket {
  private int entityId; // We only need one entityId

  @Override
  public void encode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    // No idea why Mojang decided that this is a good idea, but whatever
    if (!protocolVersion.equals(ProtocolVersion.MINECRAFT_1_17)) {
      ProtocolUtil.writeVarInt(byteBuf, 1); // size
    }

    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_8)) {
      byteBuf.writeInt(entityId);
    } else {
      ProtocolUtil.writeVarInt(byteBuf, entityId);
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }

  public int getEntityId() {
    return this.entityId;
  }

  @java.lang.Override
  public java.lang.String toString() {
    return "RemoveEntitiesPacket(entityId=" + this.getEntityId() + ")";
  }

  public RemoveEntitiesPacket() {
  }

  public RemoveEntitiesPacket(final int entityId) {
    this.entityId = entityId;
  }
}
