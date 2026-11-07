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

public final class VehicleMovePacket implements SonarPacket {
  private double x;
  private double y;
  private double z;
  private float yaw;
  private float pitch;
  private boolean onGround;

  @Override
  public void encode(final @NotNull ByteBuf byteBuf, final ProtocolVersion protocolVersion) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public void decode(final @NotNull ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    x = byteBuf.readDouble();
    y = byteBuf.readDouble();
    z = byteBuf.readDouble();
    yaw = byteBuf.readFloat();
    pitch = byteBuf.readFloat();
    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_21_4)) {
      onGround = byteBuf.readBoolean();
    }
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

  public boolean isOnGround() {
    return this.onGround;
  }

  @java.lang.Override
  public java.lang.String toString() {
    return "VehicleMovePacket(x=" + this.getX() + ", y=" + this.getY() + ", z=" + this.getZ() + ", yaw=" + this.getYaw() + ", pitch=" + this.getPitch() + ", onGround=" + this.isOnGround() + ")";
  }

  public VehicleMovePacket() {
  }

  public VehicleMovePacket(final double x, final double y, final double z, final float yaw, final float pitch, final boolean onGround) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.yaw = yaw;
    this.pitch = pitch;
    this.onGround = onGround;
  }
}
