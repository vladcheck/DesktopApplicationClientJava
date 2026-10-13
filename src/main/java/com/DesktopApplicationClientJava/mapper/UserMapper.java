package com.DesktopApplicationClientJava.mapper;

import com.DesktopApplicationClientJava.Api.Dto.response.UserDto;
import com.DesktopApplicationClientJava.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface UserMapper {

  UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

  // Имена в контракте бэкенда в нижнем регистре, в сущности — camelCase.
  @Mapping(target = "firstName", source = "firstname")
  @Mapping(target = "lastName", source = "lastname")
  User toEntity(UserDto dto);
}
