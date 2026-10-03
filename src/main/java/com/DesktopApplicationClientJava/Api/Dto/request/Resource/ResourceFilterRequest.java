package com.DesktopApplicationClientJava.Api.Dto.request.Resource;

import java.time.LocalDateTime;

public record ResourceFilterRequest(
    String title, String description, LocalDateTime createdAt, LocalDateTime updatedAt) {}
