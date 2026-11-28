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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Immutable set of IPv4 and IPv6 networks that answers {@link #contains(InetAddress)} with a binary search.
 * <p>
 * Lists of datacenter or VPN networks can contain hundreds of thousands of entries,
 * so checking them one by one (like the allowlist does) would be far too slow during an attack.
 * Every network is stored as an inclusive range of two unsigned 128-bit numbers (high and low half);
 * overlapping and adjacent ranges are merged when the set is built.
 */
public final class SubnetRangeSet {
  public static final SubnetRangeSet EMPTY = new SubnetRangeSet(new long[0][], new long[0][]);

  // Each entry is {startHigh, startLow, endHigh, endLow}, sorted by start
  private final long[][] ipv4;
  private final long[][] ipv6;

  private SubnetRangeSet(final long[][] ipv4, final long[][] ipv6) {
    this.ipv4 = ipv4;
    this.ipv6 = ipv6;
  }

  /**
   * @param ranges The networks of the set
   * @return A new set containing all given networks
   */
  public static @NotNull SubnetRangeSet of(final @NotNull Collection<SubnetRange> ranges) {
    final List<long[]> ipv4 = new ArrayList<>();
    final List<long[]> ipv6 = new ArrayList<>();
    for (final SubnetRange range : ranges) {
      final byte[] network = range.getNetwork();
      (network.length == 4 ? ipv4 : ipv6).add(toInterval(network, range.getPrefixLength()));
    }
    return new SubnetRangeSet(merge(ipv4), merge(ipv6));
  }

  /**
   * @param inetAddress The address to look up
   * @return {@code true} if the address belongs to one of the networks
   */
  public boolean contains(final @NotNull InetAddress inetAddress) {
    final byte[] address = inetAddress.getAddress();
    final long[][] intervals = address.length == 4 ? ipv4 : ipv6;
    if (intervals.length == 0) {
      return false;
    }
    final long high = toHigh(address);
    final long low = toLow(address);
    // Find the last interval that starts at or before the address
    int left = 0, right = intervals.length - 1, match = -1;
    while (left <= right) {
      final int middle = (left + right) >>> 1;
      if (compare(intervals[middle][0], intervals[middle][1], high, low) <= 0) {
        match = middle;
        left = middle + 1;
      } else {
        right = middle - 1;
      }
    }
    return match != -1 && compare(high, low, intervals[match][2], intervals[match][3]) <= 0;
  }

  /**
   * @return The number of merged networks in this set
   */
  public int size() {
    return ipv4.length + ipv6.length;
  }

  private static long @NotNull [] toInterval(final byte @NotNull [] network, final int prefixLength) {
    // Set all bits after the prefix to one to get the last address of the network
    final byte[] last = network.clone();
    for (int bit = prefixLength; bit < last.length * 8; bit++) {
      last[bit / 8] |= (byte) (0x80 >>> (bit % 8));
    }
    return new long[]{toHigh(network), toLow(network), toHigh(last), toLow(last)};
  }

  private static long @NotNull [][] merge(final @NotNull List<long[]> intervals) {
    intervals.sort(Comparator.<long[]>comparingLong(interval -> interval[0] ^ Long.MIN_VALUE)
      .thenComparingLong(interval -> interval[1] ^ Long.MIN_VALUE));
    final List<long[]> merged = new ArrayList<>(intervals.size());
    for (final long[] interval : intervals) {
      final long[] last = merged.isEmpty() ? null : merged.get(merged.size() - 1);
      if (last != null && isMergeable(last, interval)) {
        // Extend the previous interval if this one ends later
        if (compare(interval[2], interval[3], last[2], last[3]) > 0) {
          last[2] = interval[2];
          last[3] = interval[3];
        }
      } else {
        merged.add(interval);
      }
    }
    return merged.toArray(new long[0][]);
  }

  /**
   * @return {@code true} if the next interval starts inside or directly after the previous one
   */
  private static boolean isMergeable(final long @NotNull [] previous, final long @NotNull [] next) {
    if (compare(next[0], next[1], previous[2], previous[3]) <= 0) {
      return true;
    }
    // Adjacent: previous end + 1 == next start (ignoring the overflow of the very last address)
    final long low = previous[3] + 1;
    final long high = low == 0 ? previous[2] + 1 : previous[2];
    return high == next[0] && low == next[1];
  }

  private static int compare(final long highA, final long lowA, final long highB, final long lowB) {
    final int result = Long.compareUnsigned(highA, highB);
    return result != 0 ? result : Long.compareUnsigned(lowA, lowB);
  }

  // IPv4 addresses only use the low half; IPv6 addresses use both halves
  private static long toHigh(final byte @NotNull [] address) {
    return address.length == 4 ? 0L : toLong(address, 0);
  }

  private static long toLow(final byte @NotNull [] address) {
    return address.length == 4 ? toLong(address, 0) : toLong(address, 8);
  }

  private static long toLong(final byte @NotNull [] address, final int offset) {
    long value = 0L;
    for (int i = offset; i < Math.min(offset + 8, address.length); i++) {
      value = (value << 8) | (address[i] & 0xFF);
    }
    return value;
  }
}
