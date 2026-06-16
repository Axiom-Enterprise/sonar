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

package xyz.jonesdev.sonar.api.fingerprint;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class FingerprintingUtil {

  private static final long FNV_OFFSET = 0xcbf29ce484222325L;
  private static final long FNV_PRIME = 0x00000100000001B3L;

  private static long fnv1a64(final @NotNull String input) {
    long hash = FNV_OFFSET;
    for (int i = 0; i < input.length(); i++) {
      hash ^= input.charAt(i);
      hash *= FNV_PRIME;
    }
    return hash;
  }

  public @NotNull String getFingerprint(final @NotNull String username,
                                        final @NotNull String hostAddress) {
    return Long.toHexString(fnv1a64(username)) + Long.toHexString(fnv1a64(hostAddress));
  }
}
