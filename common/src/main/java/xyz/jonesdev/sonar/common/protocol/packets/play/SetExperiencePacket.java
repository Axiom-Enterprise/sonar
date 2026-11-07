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

public final class SetExperiencePacket implements SonarPacket {
  private float experienceBar;
  private int level;
  private int totalExperience;

  @Override
  public void encode(final @NotNull ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    byteBuf.writeFloat(experienceBar);

    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_8)) {
      byteBuf.writeShort(level);
      byteBuf.writeShort(totalExperience);
      return;
    }

    ProtocolUtil.writeVarInt(byteBuf, level);
    ProtocolUtil.writeVarInt(byteBuf, totalExperience);
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }

  public float getExperienceBar() {
    return this.experienceBar;
  }

  public int getLevel() {
    return this.level;
  }

  public int getTotalExperience() {
    return this.totalExperience;
  }

  @java.lang.Override
  public java.lang.String toString() {
    return "SetExperiencePacket(experienceBar=" + this.getExperienceBar() + ", level=" + this.getLevel() + ", totalExperience=" + this.getTotalExperience() + ")";
  }

  public SetExperiencePacket() {
  }

  public SetExperiencePacket(final float experienceBar, final int level, final int totalExperience) {
    this.experienceBar = experienceBar;
    this.level = level;
    this.totalExperience = totalExperience;
  }
}
