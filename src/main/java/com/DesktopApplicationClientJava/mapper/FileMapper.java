package com.DesktopApplicationClientJava.mapper;

import com.DesktopApplicationClientJava.Api.Dto.response.FileDto;
import com.DesktopApplicationClientJava.entities.FileInfo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface FileMapper {

  FileMapper INSTANCE = Mappers.getMapper(FileMapper.class);

  FileInfo toEntity(FileDto dto);
}
