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

public final class PlayerAbilitiesPacket implements SonarPacket {
  private int encodedFlags;
  private float flySpeed;
  private float walkSpeed;

  @Override
  public void encode(final @NotNull ByteBuf byteBuf, final ProtocolVersion protocolVersion) throws Exception {
    byteBuf.writeByte(encodedFlags);
    byteBuf.writeFloat(flySpeed);
    byteBuf.writeFloat(walkSpeed);
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }

  public int getEncodedFlags() {
    return this.encodedFlags;
  }

  public float getFlySpeed() {
    return this.flySpeed;
  }

  public float getWalkSpeed() {
    return this.walkSpeed;
  }

  @java.lang.Override
  public java.lang.String toString() {
    return "PlayerAbilitiesPacket(encodedFlags=" + this.getEncodedFlags() + ", flySpeed=" + this.getFlySpeed() + ", walkSpeed=" + this.getWalkSpeed() + ")";
  }

  public PlayerAbilitiesPacket() {
  }

  public PlayerAbilitiesPacket(final int encodedFlags, final float flySpeed, final float walkSpeed) {
    this.encodedFlags = encodedFlags;
    this.flySpeed = flySpeed;
    this.walkSpeed = walkSpeed;
  }
}
