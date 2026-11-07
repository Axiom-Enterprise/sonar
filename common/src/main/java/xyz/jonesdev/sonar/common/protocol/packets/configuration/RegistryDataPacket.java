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

package xyz.jonesdev.sonar.common.protocol.packets.configuration;

import io.netty.buffer.ByteBuf;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.antibot.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.protocol.SonarPacket;
import xyz.jonesdev.sonar.common.protocol.SonarPacketSnapshot;
import xyz.jonesdev.sonar.common.util.ProtocolUtil;

import java.util.ArrayList;
import java.util.List;

public final class RegistryDataPacket implements SonarPacket {
  private CompoundBinaryTag tag;
  private String type;
  private List<RegistryDataPacket.Bundle> bundles;

  @Override
  public void encode(final @NotNull ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
      ProtocolUtil.writeBinaryTag(byteBuf, protocolVersion, tag);
    } else if (type != null) {
      ProtocolUtil.writeString(byteBuf, type);
      ProtocolUtil.writeVarInt(byteBuf, bundles.size());

      for (final RegistryDataPacket.Bundle bundle : bundles) {
        ProtocolUtil.writeString(byteBuf, bundle.getName());
        // Write the bundle tag
        final BinaryTag tag = bundle.getTag();
        if (tag != null) {
          byteBuf.writeBoolean(true);
          ProtocolUtil.writeBinaryTag(byteBuf, protocolVersion, tag);
        } else {
          byteBuf.writeBoolean(false);
        }
      }
    }
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }

  public static SonarPacket @NotNull [] of(final @NotNull CompoundBinaryTag rootTag) {
    final SonarPacket[] packets = new SonarPacket[rootTag.size()];
    int index = 0;
    for (final String type : rootTag.keySet()) {
      final ArrayList<RegistryDataPacket.Bundle> bundles = new ArrayList<>();
      for (final BinaryTag binaryTag : rootTag.getCompound(type).getList("value")) {
        final CompoundBinaryTag tag = (CompoundBinaryTag) binaryTag;
        bundles.add(new Bundle(tag.getString("name"), tag.get("element")));
      }
      packets[index++] = new SonarPacketSnapshot(new RegistryDataPacket(rootTag, type, bundles));
    }
    return packets;
  }

  public static final class Bundle {
    @NotNull
    private final String name;
    // In the protocol, it is able to be null
    @Nullable
    private final BinaryTag tag;

    public Bundle(@NotNull final String name, @Nullable final BinaryTag tag) {
      if (name == null) {
        throw new java.lang.NullPointerException("name is marked non-null but is null");
      }
      this.name = name;
      this.tag = tag;
    }

    @NotNull
    public String getName() {
      return this.name;
    }

    @Nullable
    public BinaryTag getTag() {
      return this.tag;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
      if (o == this) return true;
      if (!(o instanceof RegistryDataPacket.Bundle)) return false;
      final RegistryDataPacket.Bundle other = (RegistryDataPacket.Bundle) o;
      final java.lang.Object this$name = this.getName();
      final java.lang.Object other$name = other.getName();
      if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
      final java.lang.Object this$tag = this.getTag();
      final java.lang.Object other$tag = other.getTag();
      if (this$tag == null ? other$tag != null : !this$tag.equals(other$tag)) return false;
      return true;
    }

    @java.lang.Override
    public int hashCode() {
      final int PRIME = 59;
      int result = 1;
      final java.lang.Object $name = this.getName();
      result = result * PRIME + ($name == null ? 43 : $name.hashCode());
      final java.lang.Object $tag = this.getTag();
      result = result * PRIME + ($tag == null ? 43 : $tag.hashCode());
      return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
      return "RegistryDataPacket.Bundle(name=" + this.getName() + ", tag=" + this.getTag() + ")";
    }
  }

  public CompoundBinaryTag getTag() {
    return this.tag;
  }

  public String getType() {
    return this.type;
  }

  public List<RegistryDataPacket.Bundle> getBundles() {
    return this.bundles;
  }

  public RegistryDataPacket() {
  }

  public RegistryDataPacket(final CompoundBinaryTag tag, final String type, final List<RegistryDataPacket.Bundle> bundles) {
    this.tag = tag;
    this.type = type;
    this.bundles = bundles;
  }
}
