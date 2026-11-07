/*
 * Copyright (C) 2026 Sonar Contributors
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

package xyz.jonesdev.sonar.api.antibot.subnet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Immutable representation of an IPv4 or IPv6 network in CIDR notation (e.g. {@code 10.0.0.0/8}).
 * <p>
 * Parsing never performs DNS lookups: only numeric IPv4 and IPv6 literals are accepted.
 */
public final class SubnetRange {
  private static final Pattern IPV4_LITERAL = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");
  private static final Pattern IPV6_LITERAL = Pattern.compile("^[0-9a-fA-F:.]+$");

  private final byte[] network;
  private final int prefixLength;

  private SubnetRange(final byte @NotNull [] network, final int prefixLength) {
    this.network = applyMask(network, prefixLength);
    this.prefixLength = prefixLength;
  }

  /**
   * @return A copy of the masked network address
   */
  public byte @NotNull [] getNetwork() {
    return network.clone();
  }

  /**
   * @return The number of leading bits that define the network
   */
  public int getPrefixLength() {
    return prefixLength;
  }

  /**
   * Parses a single address ({@code 1.2.3.4}) or a CIDR range ({@code 1.2.3.0/24}, {@code 2001:db8::/32}).
   * A single address is treated as a range containing only that address.
   *
   * @param raw The raw string from the configuration or a command
   * @return The parsed range or {@code null} if the input is not a valid numeric address or prefix
   */
  public static @Nullable SubnetRange parse(final @NotNull String raw) {
    final String trimmed = raw.trim();
    final int slash = trimmed.indexOf('/');
    final String address = slash == -1 ? trimmed : trimmed.substring(0, slash);
    final InetAddress inetAddress = parseLiteral(address);
    if (inetAddress == null) {
      return null;
    }
    final int maxBits = inetAddress.getAddress().length * 8;
    int prefixLength = maxBits;
    if (slash != -1) {
      try {
        prefixLength = Integer.parseInt(trimmed.substring(slash + 1));
      } catch (NumberFormatException exception) {
        return null;
      }
      if (prefixLength < 0 || prefixLength > maxBits) {
        return null;
      }
    }
    return new SubnetRange(inetAddress.getAddress(), prefixLength);
  }

  /**
   * Converts a numeric IP literal into an {@link InetAddress} without ever resolving hostnames.
   *
   * @param literal IPv4 (dotted quad) or IPv6 literal
   * @return The address or {@code null} if the literal is invalid
   */
  public static @Nullable InetAddress parseLiteral(final @NotNull String literal) {
    // IPv6 literals must contain a colon; otherwise InetAddress would try to resolve a hostname
    final boolean ipv4 = IPV4_LITERAL.matcher(literal).matches();
    final boolean ipv6 = literal.indexOf(':') != -1 && IPV6_LITERAL.matcher(literal).matches();
    if (!ipv4 && !ipv6) {
      return null;
    }
    try {
      return InetAddress.getByName(literal);
    } catch (UnknownHostException exception) {
      return null;
    }
  }

  /**
   * @param inetAddress The address to check
   * @return {@code true} if the address belongs to this network (IPv4 never matches an IPv6 range and vice versa)
   */
  public boolean contains(final @NotNull InetAddress inetAddress) {
    final byte[] address = inetAddress.getAddress();
    if (address.length != network.length) {
      return false;
    }
    final byte[] masked = applyMask(address, prefixLength);
    for (int i = 0; i < masked.length; i++) {
      if (masked[i] != network[i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Builds the key that groups all addresses of the same subnet together.
   * If the prefix covers the whole address, the plain host address is returned,
   * which keeps the key compatible with entries created before subnet support existed.
   *
   * @param inetAddress  The address of the player
   * @param prefixLength The prefix length to group by (clamped to the address size)
   * @return e.g. {@code 2001:db8:0:0:0:0:0:0/64} or {@code 1.2.3.4}
   */
  public static @NotNull String toSubnetKey(final @NotNull InetAddress inetAddress, final int prefixLength) {
    final byte[] address = inetAddress.getAddress();
    final int maxBits = address.length * 8;
    if (prefixLength >= maxBits) {
      return inetAddress.getHostAddress();
    }
    final int clampedPrefix = Math.max(prefixLength, 0);
    try {
      return InetAddress.getByAddress(applyMask(address, clampedPrefix)).getHostAddress() + "/" + clampedPrefix;
    } catch (UnknownHostException exception) {
      // Cannot happen since the byte array always has a valid length (4 or 16)
      return inetAddress.getHostAddress();
    }
  }

  /**
   * Returns a copy of the given address with all bits after the prefix set to zero.
   */
  private static byte @NotNull [] applyMask(final byte @NotNull [] address, final int prefixLength) {
    final byte[] result = address.clone();
    for (int i = 0; i < result.length; i++) {
      final int bitsInByte = Math.min(Math.max(prefixLength - i * 8, 0), 8);
      // Keep the first 'bitsInByte' bits of the current byte
      final int mask = bitsInByte == 0 ? 0 : (0xFF << (8 - bitsInByte)) & 0xFF;
      result[i] = (byte) (result[i] & mask);
    }
    return result;
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof SubnetRange)) {
      return false;
    }
    final SubnetRange range = (SubnetRange) other;
    return prefixLength == range.prefixLength && Arrays.equals(network, range.network);
  }

  @Override
  public int hashCode() {
    return 31 * Arrays.hashCode(network) + prefixLength;
  }

  @Override
  public @NotNull String toString() {
    try {
      return InetAddress.getByAddress(network).getHostAddress() + "/" + prefixLength;
    } catch (UnknownHostException exception) {
      // Cannot happen since the network always has a valid length (4 or 16)
      throw new IllegalStateException(exception);
    }
  }
}
