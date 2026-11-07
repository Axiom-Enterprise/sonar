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

public final class SetPlayerRotationPacket implements SonarPacket {
  private float yaw;
  private float pitch;
  private boolean onGround;
  private boolean horizontalCollision;

  @Override
  public void encode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void decode(final @NotNull ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    yaw = byteBuf.readFloat();
    pitch = byteBuf.readFloat();
    if (protocolVersion.greaterThan(ProtocolVersion.MINECRAFT_1_21_2)) {
      short flag = byteBuf.readUnsignedByte();
      onGround = (flag & 1) != 0;
      horizontalCollision = (flag & 2) != 0;
    } else {
      onGround = byteBuf.readBoolean();
    }
  }

  public float getYaw() {
    return this.yaw;
  }

  public float getPitch() {
    return this.pitch;
  }

  public boolean isOnGround() {
    return this.onGround;
  }

  public boolean isHorizontalCollision() {
    return this.horizontalCollision;
  }

  @java.lang.Override
  public java.lang.String toString() {
    return "SetPlayerRotationPacket(yaw=" + this.getYaw() + ", pitch=" + this.getPitch() + ", onGround=" + this.isOnGround() + ", horizontalCollision=" + this.isHorizontalCollision() + ")";
  }

  public SetPlayerRotationPacket() {
  }

  public SetPlayerRotationPacket(final float yaw, final float pitch, final boolean onGround, final boolean horizontalCollision) {
    this.yaw = yaw;
    this.pitch = pitch;
    this.onGround = onGround;
    this.horizontalCollision = horizontalCollision;
  }
}
