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

public final class GameEventPacket implements SonarPacket {
  private int type;
  private float value;

  @Override
  public void encode(final @NotNull ByteBuf byteBuf, final ProtocolVersion protocolVersion) throws Exception {
    byteBuf.writeByte(type);
    byteBuf.writeFloat(value);
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }

  public int getType() {
    return this.type;
  }

  public float getValue() {
    return this.value;
  }

  public void setType(final int type) {
    this.type = type;
  }

  public void setValue(final float value) {
    this.value = value;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object o) {
    if (o == this) return true;
    if (!(o instanceof GameEventPacket)) return false;
    final GameEventPacket other = (GameEventPacket) o;
    if (this.getType() != other.getType()) return false;
    if (java.lang.Float.compare(this.getValue(), other.getValue()) != 0) return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    final int PRIME = 59;
    int result = 1;
    result = result * PRIME + this.getType();
    result = result * PRIME + java.lang.Float.floatToIntBits(this.getValue());
    return result;
  }

  @java.lang.Override
  public java.lang.String toString() {
    return "GameEventPacket(type=" + this.getType() + ", value=" + this.getValue() + ")";
  }

  public GameEventPacket() {
  }

  public GameEventPacket(final int type, final float value) {
    this.type = type;
    this.value = value;
  }
}
