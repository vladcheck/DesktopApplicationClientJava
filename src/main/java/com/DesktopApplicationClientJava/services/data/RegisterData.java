package com.DesktopApplicationClientJava.services.data;

import com.DesktopApplicationClientJava.entities.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterData {
    private final String email;
    private final String password;
    private final Role role;
    private final String firstName;
    private final String lastName;
}