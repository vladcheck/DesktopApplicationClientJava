package com.DesktopApplicationClientJava.session;

import com.DesktopApplicationClientJava.entities.User;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Session {
    private User currentUser;

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}