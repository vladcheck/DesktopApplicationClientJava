package com.DesktopApplicationClientJava.session;

import com.DesktopApplicationClientJava.entities.User;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Session {

  private User currentUser;
  private String accessToken;
  private String refreshToken;

  // ===== user =====

  public User getCurrentUser() {
    return currentUser;
  }

  public void setCurrentUser(User user) {
    this.currentUser = user;
  }

  // ===== tokens =====

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  // ===== helpers =====

  public boolean isLoggedIn() {
    return currentUser != null && accessToken != null;
  }

  public void clearTokens() {
    this.accessToken = null;
    this.refreshToken = null;
  }

  public void clear() {
    this.currentUser = null;
    clearTokens();
  }
}
