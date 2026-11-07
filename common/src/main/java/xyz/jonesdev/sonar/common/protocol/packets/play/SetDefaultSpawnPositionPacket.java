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

public final class SetDefaultSpawnPositionPacket implements SonarPacket {
  private String dimensionName;
  private int x;
  private int y;
  private int z;

  @Override
  public void encode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_8)) {
      byteBuf.writeInt(x);
      byteBuf.writeInt(y);
      byteBuf.writeInt(z);
    } else {
      if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_21_9)) {
        ProtocolUtil.writeString(byteBuf, dimensionName);
      }

      final long encoded = protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_14)
        ? ((x & 0x3FFFFFFL) << 38) | ((y & 0xFFFL) << 26) | (z & 0x3FFFFFFL)
        : ((x & 0x3FFFFFFL) << 38) | ((y & 0x3FFFFFFL) << 12) | (z & 0xFFFL);

      byteBuf.writeLong(encoded);

      if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_17)) {
        byteBuf.writeFloat(0f);

        if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_21_9)) {
          byteBuf.writeFloat(0f); // pitch
        }
      }
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }

  public String getDimensionName() {
    return this.dimensionName;
  }

  public int getX() {
    return this.x;
  }

  public int getY() {
    return this.y;
  }

  public int getZ() {
    return this.z;
  }

  @java.lang.Override
  public java.lang.String toString() {
    return "SetDefaultSpawnPositionPacket(dimensionName=" + this.getDimensionName() + ", x=" + this.getX() + ", y=" + this.getY() + ", z=" + this.getZ() + ")";
  }

  public SetDefaultSpawnPositionPacket() {
  }

  public SetDefaultSpawnPositionPacket(final String dimensionName, final int x, final int y, final int z) {
    this.dimensionName = dimensionName;
    this.x = x;
    this.y = y;
    this.z = z;
  }
}
