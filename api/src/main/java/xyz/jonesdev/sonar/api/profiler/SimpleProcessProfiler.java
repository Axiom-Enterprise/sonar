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

package xyz.jonesdev.sonar.api.profiler;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;

public final class SimpleProcessProfiler {
  private static final Runtime RUNTIME = Runtime.getRuntime();
  private static final OperatingSystemMXBean MX = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
  private static final char[] MEMORY_UNITS = {'K', 'M', 'G', 'T', 'P', 'E'};
  private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.#");

  public static String formatMemory(final long size) {
    if (size < 1024L) {
      return size + " B";
    }
    // https://stackoverflow.com/questions/3758606/how-can-i-convert-byte-size-into-a-human-readable-format-in-java
    final int group = (63 - Long.numberOfLeadingZeros(size)) / 10;
    final double formattedSize = (double) size / (1L << (group * 10));
    final char unit = MEMORY_UNITS[group - 1];
    // https://en.wikipedia.org/wiki/Byte#Multiple-byte_units
    return DECIMAL_FORMAT.format(formattedSize) + " " + unit + "iB";
  }

  /**
   * @return Number of available processors for the JVM
   */
  public static int getVirtualCores() {
    return RUNTIME.availableProcessors();
  }

  /**
   * @return Current process CPU usage in percent
   */
  public static double getProcessCPUUsage() {
    return MX.getProcessCpuLoad() * 100;
  }

  /**
   * @return Current per-core process CPU usage in percent
   */
  public static double getAverageProcessCPUUsage() {
    return getProcessCPUUsage() / getVirtualCores();
  }

  /**
   * @return Current system CPU usage in percent
   */
  public static double getSystemCPUUsage() {
    return MX.getSystemCpuLoad() * 100;
  }

  /**
   * @return Current per-core system CPU usage in percent
   */
  public static double getAverageSystemCPUUsage() {
    return getSystemCPUUsage() / getVirtualCores();
  }

  /**
   * @return Memory (in bytes) the JVM will attempt to use
   */
  public static long getMaxMemory() {
    return RUNTIME.maxMemory();
  }

  /**
   * @return Memory (in bytes) available to the host machine
   */
  public static long getTotalMemory() {
    return RUNTIME.totalMemory();
  }

  /**
   * @return Memory (in bytes) the JVM can allocate
   */
  public static long getFreeMemory() {
    return RUNTIME.freeMemory();
  }

  /**
   * @return Memory (in bytes) the JVM is using
   */
  public static long getUsedMemory() {
    return getTotalMemory() - getFreeMemory();
  }

  private SimpleProcessProfiler() {
    throw new java.lang.UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }
}
