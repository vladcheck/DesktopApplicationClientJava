package com.DesktopApplicationClientJava.Api.Dto.request.Admin;
import com.DesktopApplicationClientJava.entities.Role;

public record RegisterRequest(
        String email,
        String password,
        Role role,       // "USER", "MODER" или "ADMIN"
        String firstname,
        String lastname
) {}
