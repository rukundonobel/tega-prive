package com.tegaprive.auth;

import com.tegaprive.auth.dto.UserDto;
import com.tegaprive.users.User;

/** Maps User entities to their public DTO projection. */
public final class UserMapper {

  private UserMapper() {
  }

  public static UserDto toDto(User user) {
    return new UserDto(
        user.getId().toString(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.getPhone(),
        user.getRoles().stream().map(Enum::name).toList(),
        user.getStatus().name());
  }
}
