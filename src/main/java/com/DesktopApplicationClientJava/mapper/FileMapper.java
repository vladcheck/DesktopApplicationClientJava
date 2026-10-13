package com.DesktopApplicationClientJava.mapper;

import com.DesktopApplicationClientJava.Api.Dto.response.FileDto;
import com.DesktopApplicationClientJava.entities.FileInfo;

public final class FileMapper {

  private FileMapper() {}

  public static FileInfo toEntity(FileDto dto) {
    return new FileInfo(dto.uuid(), dto.name(), dto.contentType(), dto.size());
  }
}
