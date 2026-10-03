package com.DesktopApplicationClientJava.Api.Dto.request.Admin;

import com.DesktopApplicationClientJava.entities.Role;

public record UpdateUserRequest(
        String email,
        Boolean enabled,
        Role role,
        String firstname,
        String lastname
) {}
