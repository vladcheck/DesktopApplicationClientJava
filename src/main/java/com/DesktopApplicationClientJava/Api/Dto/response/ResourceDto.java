package com.DesktopApplicationClientJava.Api.Dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ResourceDto(
        UUID uuid,
        String title,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<FileDto> files
) {}
