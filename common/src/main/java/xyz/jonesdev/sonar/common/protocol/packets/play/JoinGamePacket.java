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
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.antibot.protocol.ProtocolVersion;
import xyz.jonesdev.sonar.common.protocol.SonarPacket;
import xyz.jonesdev.sonar.common.protocol.dimension.DimensionRegistry;
import xyz.jonesdev.sonar.common.protocol.dimension.DimensionType;
import xyz.jonesdev.sonar.common.util.ProtocolUtil;

import java.util.Objects;

public final class JoinGamePacket implements SonarPacket {
  private int entityId;
  private int gamemode;
  private int previousGamemode;
  private int viewDistance;
  private int simulationDistance;
  private int difficulty;
  private int maxPlayers;
  private int portalCooldown;
  private int seaLevel;
  private String[] levelNames;
  private String levelName;
  private String levelType;
  private DimensionType dimension;
  private long partialHashedSeed;
  private boolean hardcore;
  private boolean reducedDebugInfo;
  private boolean showRespawnScreen;
  private boolean debug;
  private boolean flat;
  private boolean limitedCrafting;
  private boolean secureProfile;
  private boolean onlineMode;

  @Override
  public void encode(final @NotNull ByteBuf byteBuf, final @NotNull ProtocolVersion protocolVersion) throws Exception {
    byteBuf.writeInt(entityId);

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_16_2)) {
      byteBuf.writeBoolean(hardcore);
    }

    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
      byteBuf.writeByte(gamemode);
    }

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_16)) {
      if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
        byteBuf.writeByte(previousGamemode);
      }

      ProtocolUtil.writeStringArray(byteBuf, levelNames);

      final CompoundBinaryTag codec = getCodec(protocolVersion);

      if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
        ProtocolUtil.writeBinaryTag(byteBuf, protocolVersion, codec);
      }

      if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_16_2)
        && protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_19)) {
        final ListBinaryTag dimensions = codec.getCompound("minecraft:dimension_type").getList("value");
        final BinaryTag elementTag = ((CompoundBinaryTag) dimensions.get(0)).get("element");
        ProtocolUtil.writeBinaryTag(byteBuf, protocolVersion, Objects.requireNonNull(elementTag));
      } else if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
        ProtocolUtil.writeString(byteBuf, dimension.getKey());
      }
      if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
        ProtocolUtil.writeString(byteBuf, levelName);
      }
    } else if (protocolVersion.greaterThan(ProtocolVersion.MINECRAFT_1_9)) {
      byteBuf.writeInt(dimension.getLegacyId());
    } else {
      byteBuf.writeByte(dimension.getLegacyId());
    }

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_15)
      && protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
      byteBuf.writeLong(partialHashedSeed);
    }

    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_14)) {
      byteBuf.writeByte(difficulty);
    }

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_16_2)) {
      ProtocolUtil.writeVarInt(byteBuf, maxPlayers);
    } else {
      byteBuf.writeByte(maxPlayers);
    }

    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_16)) {
      ProtocolUtil.writeString(byteBuf, levelType);
    }

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_14)) {
      ProtocolUtil.writeVarInt(byteBuf, viewDistance);
    }

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_18)) {
      ProtocolUtil.writeVarInt(byteBuf, simulationDistance);
    }

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_8)) {
      byteBuf.writeBoolean(reducedDebugInfo);
    }

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_15)) {
      byteBuf.writeBoolean(showRespawnScreen);
    }

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_20_2)) {
      byteBuf.writeBoolean(limitedCrafting);

      if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_20_5)) {
        ProtocolUtil.writeVarInt(byteBuf, dimension.getId());
      } else {
        ProtocolUtil.writeString(byteBuf, dimension.getKey());
      }

      ProtocolUtil.writeString(byteBuf, levelName);
      byteBuf.writeLong(partialHashedSeed);
      if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_26_3)) {
        ProtocolUtil.writeVarInt(byteBuf, gamemode);
        // ByteBufCodecs#OPTIONAL_VAR_INT: 0 for empty, (value + 1) if present
        ProtocolUtil.writeVarInt(byteBuf, previousGamemode == -1 ? 0 : previousGamemode + 1);
      } else {
        byteBuf.writeByte(gamemode);
        byteBuf.writeByte(previousGamemode);
      }
    }

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_16)) {
      byteBuf.writeBoolean(debug);
      byteBuf.writeBoolean(flat);
    }

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_19)) {
      byteBuf.writeBoolean(false); // last death location
    }

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_20)) {
      ProtocolUtil.writeVarInt(byteBuf, portalCooldown);
    }

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_21_2)) {
      ProtocolUtil.writeVarInt(byteBuf, seaLevel);
    }

    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_26_2)) {
      byteBuf.writeBoolean(onlineMode);
      byteBuf.writeBoolean(secureProfile);
    } else if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_20_5)) {
      byteBuf.writeBoolean(secureProfile);
    }
  }

  private static CompoundBinaryTag getCodec(final @NotNull ProtocolVersion protocolVersion) {
    if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_20)) {
      return DimensionRegistry.CODEC_1_20;
    } else if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_19_4)) {
      return DimensionRegistry.CODEC_1_19_4;
    } else if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_19_1)) {
      return DimensionRegistry.CODEC_1_19_1;
    } else if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_19)) {
      return DimensionRegistry.CODEC_1_19;
    } else if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_18_2)) {
      return DimensionRegistry.CODEC_1_18_2;
    } else if (protocolVersion.greaterThanOrEquals(ProtocolVersion.MINECRAFT_1_16_2)) {
      return DimensionRegistry.CODEC_1_16_2;
    }
    return DimensionRegistry.CODEC_1_16;
  }

  @Override
  public void decode(final ByteBuf byteBuf, final ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }

  public int getEntityId() {
    return this.entityId;
  }

  public int getGamemode() {
    return this.gamemode;
  }

  public int getPreviousGamemode() {
    return this.previousGamemode;
  }

  public int getViewDistance() {
    return this.viewDistance;
  }

  public int getSimulationDistance() {
    return this.simulationDistance;
  }

  public int getDifficulty() {
    return this.difficulty;
  }

  public int getMaxPlayers() {
    return this.maxPlayers;
  }

  public int getPortalCooldown() {
    return this.portalCooldown;
  }

  public int getSeaLevel() {
    return this.seaLevel;
  }

  public String[] getLevelNames() {
    return this.levelNames;
  }

  public String getLevelName() {
    return this.levelName;
  }

  public String getLevelType() {
    return this.levelType;
  }

  public DimensionType getDimension() {
    return this.dimension;
  }

  public long getPartialHashedSeed() {
    return this.partialHashedSeed;
  }

  public boolean isHardcore() {
    return this.hardcore;
  }

  public boolean isReducedDebugInfo() {
    return this.reducedDebugInfo;
  }

  public boolean isShowRespawnScreen() {
    return this.showRespawnScreen;
  }

  public boolean isDebug() {
    return this.debug;
  }

  public boolean isFlat() {
    return this.flat;
  }

  public boolean isLimitedCrafting() {
    return this.limitedCrafting;
  }

  public boolean isSecureProfile() {
    return this.secureProfile;
  }

  public boolean isOnlineMode() {
    return this.onlineMode;
  }

  public void setEntityId(final int entityId) {
    this.entityId = entityId;
  }

  public void setGamemode(final int gamemode) {
    this.gamemode = gamemode;
  }

  public void setPreviousGamemode(final int previousGamemode) {
    this.previousGamemode = previousGamemode;
  }

  public void setViewDistance(final int viewDistance) {
    this.viewDistance = viewDistance;
  }

  public void setSimulationDistance(final int simulationDistance) {
    this.simulationDistance = simulationDistance;
  }

  public void setDifficulty(final int difficulty) {
    this.difficulty = difficulty;
  }

  public void setMaxPlayers(final int maxPlayers) {
    this.maxPlayers = maxPlayers;
  }

  public void setPortalCooldown(final int portalCooldown) {
    this.portalCooldown = portalCooldown;
  }

  public void setSeaLevel(final int seaLevel) {
    this.seaLevel = seaLevel;
  }

  public void setLevelNames(final String[] levelNames) {
    this.levelNames = levelNames;
  }

  public void setLevelName(final String levelName) {
    this.levelName = levelName;
  }

  public void setLevelType(final String levelType) {
    this.levelType = levelType;
  }

  public void setDimension(final DimensionType dimension) {
    this.dimension = dimension;
  }

  public void setPartialHashedSeed(final long partialHashedSeed) {
    this.partialHashedSeed = partialHashedSeed;
  }

  public void setHardcore(final boolean hardcore) {
    this.hardcore = hardcore;
  }

  public void setReducedDebugInfo(final boolean reducedDebugInfo) {
    this.reducedDebugInfo = reducedDebugInfo;
  }

  public void setShowRespawnScreen(final boolean showRespawnScreen) {
    this.showRespawnScreen = showRespawnScreen;
  }

  public void setDebug(final boolean debug) {
    this.debug = debug;
  }

  public void setFlat(final boolean flat) {
    this.flat = flat;
  }

  public void setLimitedCrafting(final boolean limitedCrafting) {
    this.limitedCrafting = limitedCrafting;
  }

  public void setSecureProfile(final boolean secureProfile) {
    this.secureProfile = secureProfile;
  }

  public void setOnlineMode(final boolean onlineMode) {
    this.onlineMode = onlineMode;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object o) {
    if (o == this) return true;
    if (!(o instanceof JoinGamePacket)) return false;
    final JoinGamePacket other = (JoinGamePacket) o;
    if (this.getEntityId() != other.getEntityId()) return false;
    if (this.getGamemode() != other.getGamemode()) return false;
    if (this.getPreviousGamemode() != other.getPreviousGamemode()) return false;
    if (this.getViewDistance() != other.getViewDistance()) return false;
    if (this.getSimulationDistance() != other.getSimulationDistance()) return false;
    if (this.getDifficulty() != other.getDifficulty()) return false;
    if (this.getMaxPlayers() != other.getMaxPlayers()) return false;
    if (this.getPortalCooldown() != other.getPortalCooldown()) return false;
    if (this.getSeaLevel() != other.getSeaLevel()) return false;
    if (this.getPartialHashedSeed() != other.getPartialHashedSeed()) return false;
    if (this.isHardcore() != other.isHardcore()) return false;
    if (this.isReducedDebugInfo() != other.isReducedDebugInfo()) return false;
    if (this.isShowRespawnScreen() != other.isShowRespawnScreen()) return false;
    if (this.isDebug() != other.isDebug()) return false;
    if (this.isFlat() != other.isFlat()) return false;
    if (this.isLimitedCrafting() != other.isLimitedCrafting()) return false;
    if (this.isSecureProfile() != other.isSecureProfile()) return false;
    if (this.isOnlineMode() != other.isOnlineMode()) return false;
    if (!java.util.Arrays.deepEquals(this.getLevelNames(), other.getLevelNames())) return false;
    final java.lang.Object this$levelName = this.getLevelName();
    final java.lang.Object other$levelName = other.getLevelName();
    if (this$levelName == null ? other$levelName != null : !this$levelName.equals(other$levelName)) return false;
    final java.lang.Object this$levelType = this.getLevelType();
    final java.lang.Object other$levelType = other.getLevelType();
    if (this$levelType == null ? other$levelType != null : !this$levelType.equals(other$levelType)) return false;
    final java.lang.Object this$dimension = this.getDimension();
    final java.lang.Object other$dimension = other.getDimension();
    if (this$dimension == null ? other$dimension != null : !this$dimension.equals(other$dimension)) return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    final int PRIME = 59;
    int result = 1;
    result = result * PRIME + this.getEntityId();
    result = result * PRIME + this.getGamemode();
    result = result * PRIME + this.getPreviousGamemode();
    result = result * PRIME + this.getViewDistance();
    result = result * PRIME + this.getSimulationDistance();
    result = result * PRIME + this.getDifficulty();
    result = result * PRIME + this.getMaxPlayers();
    result = result * PRIME + this.getPortalCooldown();
    result = result * PRIME + this.getSeaLevel();
    final long $partialHashedSeed = this.getPartialHashedSeed();
    result = result * PRIME + (int) ($partialHashedSeed >>> 32 ^ $partialHashedSeed);
    result = result * PRIME + (this.isHardcore() ? 79 : 97);
    result = result * PRIME + (this.isReducedDebugInfo() ? 79 : 97);
    result = result * PRIME + (this.isShowRespawnScreen() ? 79 : 97);
    result = result * PRIME + (this.isDebug() ? 79 : 97);
    result = result * PRIME + (this.isFlat() ? 79 : 97);
    result = result * PRIME + (this.isLimitedCrafting() ? 79 : 97);
    result = result * PRIME + (this.isSecureProfile() ? 79 : 97);
    result = result * PRIME + (this.isOnlineMode() ? 79 : 97);
    result = result * PRIME + java.util.Arrays.deepHashCode(this.getLevelNames());
    final java.lang.Object $levelName = this.getLevelName();
    result = result * PRIME + ($levelName == null ? 43 : $levelName.hashCode());
    final java.lang.Object $levelType = this.getLevelType();
    result = result * PRIME + ($levelType == null ? 43 : $levelType.hashCode());
    final java.lang.Object $dimension = this.getDimension();
    result = result * PRIME + ($dimension == null ? 43 : $dimension.hashCode());
    return result;
  }

  @java.lang.Override
  public java.lang.String toString() {
    return "JoinGamePacket(entityId=" + this.getEntityId() + ", gamemode=" + this.getGamemode() + ", previousGamemode=" + this.getPreviousGamemode() + ", viewDistance=" + this.getViewDistance() + ", simulationDistance=" + this.getSimulationDistance() + ", difficulty=" + this.getDifficulty() + ", maxPlayers=" + this.getMaxPlayers() + ", portalCooldown=" + this.getPortalCooldown() + ", seaLevel=" + this.getSeaLevel() + ", levelNames=" + java.util.Arrays.deepToString(this.getLevelNames()) + ", levelName=" + this.getLevelName() + ", levelType=" + this.getLevelType() + ", dimension=" + this.getDimension() + ", partialHashedSeed=" + this.getPartialHashedSeed() + ", hardcore=" + this.isHardcore() + ", reducedDebugInfo=" + this.isReducedDebugInfo() + ", showRespawnScreen=" + this.isShowRespawnScreen() + ", debug=" + this.isDebug() + ", flat=" + this.isFlat() + ", limitedCrafting=" + this.isLimitedCrafting() + ", secureProfile=" + this.isSecureProfile() + ", onlineMode=" + this.isOnlineMode() + ")";
  }

  public JoinGamePacket() {
  }

  public JoinGamePacket(final int entityId, final int gamemode, final int previousGamemode, final int viewDistance, final int simulationDistance, final int difficulty, final int maxPlayers, final int portalCooldown, final int seaLevel, final String[] levelNames, final String levelName, final String levelType, final DimensionType dimension, final long partialHashedSeed, final boolean hardcore, final boolean reducedDebugInfo, final boolean showRespawnScreen, final boolean debug, final boolean flat, final boolean limitedCrafting, final boolean secureProfile, final boolean onlineMode) {
    this.entityId = entityId;
    this.gamemode = gamemode;
    this.previousGamemode = previousGamemode;
    this.viewDistance = viewDistance;
    this.simulationDistance = simulationDistance;
    this.difficulty = difficulty;
    this.maxPlayers = maxPlayers;
    this.portalCooldown = portalCooldown;
    this.seaLevel = seaLevel;
    this.levelNames = levelNames;
    this.levelName = levelName;
    this.levelType = levelType;
    this.dimension = dimension;
    this.partialHashedSeed = partialHashedSeed;
    this.hardcore = hardcore;
    this.reducedDebugInfo = reducedDebugInfo;
    this.showRespawnScreen = showRespawnScreen;
    this.debug = debug;
    this.flat = flat;
    this.limitedCrafting = limitedCrafting;
    this.secureProfile = secureProfile;
    this.onlineMode = onlineMode;
  }
}
