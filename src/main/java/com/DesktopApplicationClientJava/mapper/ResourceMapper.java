package com.DesktopApplicationClientJava.mapper;

import com.DesktopApplicationClientJava.Api.Dto.response.ResourceDto;
import com.DesktopApplicationClientJava.entities.FileInfo;
import com.DesktopApplicationClientJava.entities.Resource;
import java.util.List;

public final class ResourceMapper {

  private ResourceMapper() {}

  public static Resource toEntity(ResourceDto dto) {
    List<FileInfo> files =
        dto.files() == null ? List.of() : dto.files().stream().map(FileMapper::toEntity).toList();
    return new Resource(
        dto.uuid(), dto.title(), dto.description(), dto.createdAt(), dto.updatedAt(), files);
  }
}
