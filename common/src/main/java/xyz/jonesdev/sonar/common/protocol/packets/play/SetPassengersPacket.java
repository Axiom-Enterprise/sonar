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

public final class SetPassengersPacket implements SonarPacket {
  private int entityId;
  private int passengerId;

  @Override
  public void encode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    // You can find this in the EntityAttach packet,
    // which was later replaced by SetPassengers in 1.9+
    if (protocolVersion.lessThanOrEquals(ProtocolVersion.MINECRAFT_1_8)) {
      byteBuf.writeInt(passengerId);
      byteBuf.writeInt(entityId);
      byteBuf.writeByte(0); // leash
      return;
    }

    ProtocolUtil.writeVarInt(byteBuf, entityId);
    ProtocolUtil.writeVarInt(byteBuf, 1); // passenger count
    ProtocolUtil.writeVarInt(byteBuf, passengerId);
  }

  @Override
  public void decode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }

  public int getEntityId() {
    return this.entityId;
  }

  public int getPassengerId() {
    return this.passengerId;
  }

  @java.lang.Override
  public java.lang.String toString() {
    return "SetPassengersPacket(entityId=" + this.getEntityId() + ", passengerId=" + this.getPassengerId() + ")";
  }

  public SetPassengersPacket() {
  }

  public SetPassengersPacket(final int entityId, final int passengerId) {
    this.entityId = entityId;
    this.passengerId = passengerId;
  }
}
