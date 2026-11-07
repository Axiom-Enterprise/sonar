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

package xyz.jonesdev.sonar.api.event.impl;

import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.antibot.SonarUser;
import xyz.jonesdev.sonar.api.event.SonarEvent;

public final class UserVerifyFailedEvent implements SonarEvent {
  private final @NotNull SonarUser user;
  private final @NotNull String reason;

  @NotNull
  public SonarUser getUser() {
    return this.user;
  }

  @NotNull
  public String getReason() {
    return this.reason;
  }

  @java.lang.Override
  public java.lang.String toString() {
    return "UserVerifyFailedEvent(user=" + this.getUser() + ", reason=" + this.getReason() + ")";
  }

  public UserVerifyFailedEvent(@NotNull final SonarUser user, @NotNull final String reason) {
    if (user == null) {
      throw new java.lang.NullPointerException("user is marked non-null but is null");
    }
    if (reason == null) {
      throw new java.lang.NullPointerException("reason is marked non-null but is null");
    }
    this.user = user;
    this.reason = reason;
  }
}
