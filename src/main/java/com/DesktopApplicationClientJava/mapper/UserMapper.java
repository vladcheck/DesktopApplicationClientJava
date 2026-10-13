package com.DesktopApplicationClientJava.mapper;

import com.DesktopApplicationClientJava.Api.Dto.response.UserDto;
import com.DesktopApplicationClientJava.entities.User;

public final class UserMapper {

  private UserMapper() {}

  public static User toEntity(UserDto dto) {
    return new User(
        dto.uuid(), dto.email(), dto.role(), dto.firstname(), dto.lastname(), dto.enabled());
  }
}
