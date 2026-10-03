package com.DesktopApplicationClientJava.Api.Dto.response;

import com.DesktopApplicationClientJava.entities.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserDto(
        UUID uuid,
        String email,
        Role role,
        String firstName,
        String lastName,
        Boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}