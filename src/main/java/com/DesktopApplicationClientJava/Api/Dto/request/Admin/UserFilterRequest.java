package com.DesktopApplicationClientJava.Api.Dto.request.Admin;

import com.DesktopApplicationClientJava.entities.Role;
import java.time.LocalDateTime;

public record UserFilterRequest(
    String email,
    Role role,
    String firstName,
    String lastName,
    Boolean enabled,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
