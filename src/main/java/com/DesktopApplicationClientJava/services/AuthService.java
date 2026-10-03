package com.DesktopApplicationClientJava.services;

import com.DesktopApplicationClientJava.entities.Role;
import com.DesktopApplicationClientJava.entities.User;

public interface AuthService {

    /** Логин. При успехе user попадает в Session. */
    User login(String email, String password);

    /** Выход. Сбрасывает Session. */
    void logout();

    /** Текущий пользователь из Session (или null). */
    User getCurrentUser();

    /** Проверка роли текущего пользователя. */
    boolean hasRole(Role required);

    boolean isAdmin();
    boolean isModerator();
}