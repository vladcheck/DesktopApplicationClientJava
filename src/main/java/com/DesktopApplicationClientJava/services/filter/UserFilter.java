package com.DesktopApplicationClientJava.services.filter;

import com.DesktopApplicationClientJava.entities.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserFilter {
    private final String email;
    private final Role role;
    private final String firstName;
    private final String lastName;
    private final Boolean enabled;

    public static UserFilter empty() {
        return new UserFilter(null, null, null, null, null);
    }
}
