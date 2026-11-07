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

public final class ClientInformationPacket implements SonarPacket {
  private String locale;
  private byte viewDistance;
  private int chatVisibility;
  private boolean chatColors;
  private byte difficulty; // 1.7
  private short skinParts;
  private int mainHand;
  private boolean chatFilteringEnabled; // Added in 1.17
  private boolean clientListingAllowed; // Added in 1.18, overwrites server-list "anonymous" mode
  private int particleStatus;

  @Override
  public void encode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void decode(final ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    locale = ProtocolUtil.readString(byteBuf, 16);
    viewDistance = byteBuf.readByte();
    chatVisibility = ProtocolUtil.readVarInt(byteBuf);
    chatColors = byteBuf.readBoolean();

    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_8)) {
      difficulty = byteBuf.readByte();
    }

    skinParts = byteBuf.readUnsignedByte();

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_9)) {
      mainHand = ProtocolUtil.readVarInt(byteBuf);

      if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_17)) {
        chatFilteringEnabled = byteBuf.readBoolean();

        if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_18)) {
          clientListingAllowed = byteBuf.readBoolean();

          if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_21_2)) {
            particleStatus = ProtocolUtil.readVarInt(byteBuf);
          }
        }
      }
    }
  }

  @Override
  public int expectedMinLength(final ProtocolVersion protocolVersion) {
    return 2;
  }

  @Override
  public int expectedMaxLength(final ProtocolVersion protocolVersion) {
    return 0xff + 1; // 256 as a hard-limit
  }

  public String getLocale() {
    return this.locale;
  }

  public byte getViewDistance() {
    return this.viewDistance;
  }

  public int getChatVisibility() {
    return this.chatVisibility;
  }

  public boolean isChatColors() {
    return this.chatColors;
  }

  public byte getDifficulty() {
    return this.difficulty;
  }

  public short getSkinParts() {
    return this.skinParts;
  }

  public int getMainHand() {
    return this.mainHand;
  }

  public boolean isChatFilteringEnabled() {
    return this.chatFilteringEnabled;
  }

  public boolean isClientListingAllowed() {
    return this.clientListingAllowed;
  }

  public int getParticleStatus() {
    return this.particleStatus;
  }

  @java.lang.Override
  public java.lang.String toString() {
    return "ClientInformationPacket(locale=" + this.getLocale() + ", viewDistance=" + this.getViewDistance() + ", chatVisibility=" + this.getChatVisibility() + ", chatColors=" + this.isChatColors() + ", difficulty=" + this.getDifficulty() + ", skinParts=" + this.getSkinParts() + ", mainHand=" + this.getMainHand() + ", chatFilteringEnabled=" + this.isChatFilteringEnabled() + ", clientListingAllowed=" + this.isClientListingAllowed() + ", particleStatus=" + this.getParticleStatus() + ")";
  }

  public ClientInformationPacket() {
  }

  public ClientInformationPacket(final String locale, final byte viewDistance, final int chatVisibility, final boolean chatColors, final byte difficulty, final short skinParts, final int mainHand, final boolean chatFilteringEnabled, final boolean clientListingAllowed, final int particleStatus) {
    this.locale = locale;
    this.viewDistance = viewDistance;
    this.chatVisibility = chatVisibility;
    this.chatColors = chatColors;
    this.difficulty = difficulty;
    this.skinParts = skinParts;
    this.mainHand = mainHand;
    this.chatFilteringEnabled = chatFilteringEnabled;
    this.clientListingAllowed = clientListingAllowed;
    this.particleStatus = particleStatus;
  }
}
