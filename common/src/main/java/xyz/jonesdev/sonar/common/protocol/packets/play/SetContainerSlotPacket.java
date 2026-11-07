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
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.antibot.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.protocol.SonarPacket;
import xyz.jonesdev.sonar.common.protocol.item.ItemType;
import xyz.jonesdev.sonar.common.util.ProtocolUtil;

public final class SetContainerSlotPacket implements SonarPacket {
  private int windowId;
  private int slot;
  private int count;
  private ItemType itemType;
  private CompoundBinaryTag compoundBinaryTag;

  @Override
  public void encode(final @NotNull ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_21_2)) {
      ProtocolUtil.writeVarInt(byteBuf, windowId);
    } else {
      byteBuf.writeByte(windowId);
    }

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_17_1)) {
      ProtocolUtil.writeVarInt(byteBuf, 0);
    }

    byteBuf.writeShort(slot);

    if (protocolVersion.inBetween(ProtocolVersion.MINECRAFT_1_13_2, ProtocolVersion.MINECRAFT_1_20_3)) {
      byteBuf.writeBoolean(true);
    }

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_20_5)) {
      ProtocolUtil.writeVarInt(byteBuf, count);
    }

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_13_2)) {
      ProtocolUtil.writeVarInt(byteBuf, itemType.getId().apply(protocolVersion));
    } else {
      byteBuf.writeShort(itemType.getId().apply(protocolVersion));
    }

    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
      byteBuf.writeByte(count);
    }

    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_13)) {
      byteBuf.writeShort(0); // data
    }

    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_17)) {
      if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_8)) {
        byteBuf.writeShort(-1);
      } else {
        byteBuf.writeByte(0);
      }
    } else if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
      ProtocolUtil.writeBinaryTag(byteBuf, protocolVersion, compoundBinaryTag);
    } else {
      ProtocolUtil.writeVarInt(byteBuf, 1); // addedComponentCount
      ProtocolUtil.writeVarInt(byteBuf, 0); // removedComponentCount
      ProtocolUtil.writeVarInt(byteBuf, itemType.getComponents().apply(protocolVersion));
      ProtocolUtil.writeVarInt(byteBuf, 0); // data
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }

  public int getWindowId() {
    return this.windowId;
  }

  public int getSlot() {
    return this.slot;
  }

  public int getCount() {
    return this.count;
  }

  public ItemType getItemType() {
    return this.itemType;
  }

  public CompoundBinaryTag getCompoundBinaryTag() {
    return this.compoundBinaryTag;
  }

  @java.lang.Override
  public java.lang.String toString() {
    return "SetContainerSlotPacket(windowId=" + this.getWindowId() + ", slot=" + this.getSlot() + ", count=" + this.getCount() + ", itemType=" + this.getItemType() + ", compoundBinaryTag=" + this.getCompoundBinaryTag() + ")";
  }

  public SetContainerSlotPacket() {
  }

  public SetContainerSlotPacket(final int windowId, final int slot, final int count, final ItemType itemType, final CompoundBinaryTag compoundBinaryTag) {
    this.windowId = windowId;
    this.slot = slot;
    this.count = count;
    this.itemType = itemType;
    this.compoundBinaryTag = compoundBinaryTag;
  }
}
