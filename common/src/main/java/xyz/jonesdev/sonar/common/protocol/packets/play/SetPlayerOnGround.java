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

public final class SetPlayerOnGround implements SonarPacket {
  private boolean onGround;
  private boolean horizontalCollision;

  @Override
  public void encode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void decode(final @NotNull ByteBuf byteBuf, final ProtocolVersion protocolVersion) throws Exception {
    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_21_2)) {
      final short flag = byteBuf.readUnsignedByte();
      onGround = (flag & 1) != 0;
      horizontalCollision = (flag & 2) != 0;
    } else {
      onGround = byteBuf.readBoolean();
    }
  }

  public boolean isOnGround() {
    return this.onGround;
  }

  public boolean isHorizontalCollision() {
    return this.horizontalCollision;
  }

  @java.lang.Override
  public java.lang.String toString() {
    return "SetPlayerOnGround(onGround=" + this.isOnGround() + ", horizontalCollision=" + this.isHorizontalCollision() + ")";
  }

  public SetPlayerOnGround() {
  }

  public SetPlayerOnGround(final boolean onGround, final boolean horizontalCollision) {
    this.onGround = onGround;
    this.horizontalCollision = horizontalCollision;
  }
}
