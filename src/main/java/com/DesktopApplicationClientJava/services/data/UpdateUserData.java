package com.DesktopApplicationClientJava.services.data;

import com.DesktopApplicationClientJava.entities.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UpdateUserData {
    private final String email;
    private final Role role;
    private final String firstName;
    private final String lastName;
    private final Boolean enabled;
}