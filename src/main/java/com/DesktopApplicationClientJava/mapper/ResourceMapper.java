package com.DesktopApplicationClientJava.mapper;

import com.DesktopApplicationClientJava.Api.Dto.response.ResourceDto;
import com.DesktopApplicationClientJava.entities.Resource;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.factory.Mappers;

@Mapper(
    uses = FileMapper.class,
    // Бэкенд присылает null вместо пустого списка файлов.
    nullValueIterableMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface ResourceMapper {

  ResourceMapper INSTANCE = Mappers.getMapper(ResourceMapper.class);

  Resource toEntity(ResourceDto dto);
}
