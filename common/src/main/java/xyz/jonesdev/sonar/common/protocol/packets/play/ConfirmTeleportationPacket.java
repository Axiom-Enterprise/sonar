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
import xyz.jonesdev.sonar.api.antibot.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.protocol.SonarPacket;
import xyz.jonesdev.sonar.common.util.ProtocolUtil;

public final class ConfirmTeleportationPacket implements SonarPacket {
  private int teleportId;
  private double x;
  private double y;
  private double z;
  private float yaw;
  private float pitch;

  public ConfirmTeleportationPacket(final int teleportId) {
    this.teleportId = teleportId;
  }

  @Override
  public void encode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) throws Exception {
    teleportId = ProtocolUtil.readVarInt(byteBuf);
    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_26_3)) {
      x = byteBuf.readDouble();
      y = byteBuf.readDouble();
      z = byteBuf.readDouble();
      yaw = byteBuf.readFloat();
      pitch = byteBuf.readFloat();
    }
  }

  public int getTeleportId() {
    return this.teleportId;
  }

  public double getX() {
    return this.x;
  }

  public double getY() {
    return this.y;
  }

  public double getZ() {
    return this.z;
  }

  public float getYaw() {
    return this.yaw;
  }

  public float getPitch() {
    return this.pitch;
  }

  @java.lang.Override
  public java.lang.String toString() {
    return "ConfirmTeleportationPacket(teleportId=" + this.getTeleportId() + ", x=" + this.getX() + ", y=" + this.getY() + ", z=" + this.getZ() + ", yaw=" + this.getYaw() + ", pitch=" + this.getPitch() + ")";
  }

  public ConfirmTeleportationPacket() {
  }

  public ConfirmTeleportationPacket(final int teleportId, final double x, final double y, final double z, final float yaw, final float pitch) {
    this.teleportId = teleportId;
    this.x = x;
    this.y = y;
    this.z = z;
    this.yaw = yaw;
    this.pitch = pitch;
  }
}
