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

package xyz.jonesdev.sonar.common.protocol.block;

import org.jetbrains.annotations.NotNull;

public final class BlockUpdate {
  private final BlockPosition position;
  private final BlockType blockType;
  private final int legacyBlockState;
  private final int blockState;

  public BlockUpdate(final @NotNull BlockPosition position, final BlockType blockType) {
    this.position = position;
    this.blockType = blockType;
    final int x = position.getX() - (position.getChunkX() << 4);
    final int y = position.getY();
    final int z = position.getZ() - (position.getChunkZ() << 4);
    this.legacyBlockState = x << 12 | z << 8 | y;
    this.blockState = x << 8 | z << 4 | y - ((y >> 4) << 4);
  }

  public static final class BlockPosition {
    private final int x;
    private final int y;
    private final int z;
    private final int chunkX;
    private final int chunkZ;

    public int getX() {
      return this.x;
    }

    public int getY() {
      return this.y;
    }

    public int getZ() {
      return this.z;
    }

    public int getChunkX() {
      return this.chunkX;
    }

    public int getChunkZ() {
      return this.chunkZ;
    }

    public BlockPosition(final int x, final int y, final int z, final int chunkX, final int chunkZ) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.chunkX = chunkX;
      this.chunkZ = chunkZ;
    }
  }

  public BlockPosition getPosition() {
    return this.position;
  }

  public BlockType getBlockType() {
    return this.blockType;
  }

  public int getLegacyBlockState() {
    return this.legacyBlockState;
  }

  public int getBlockState() {
    return this.blockState;
  }
}
