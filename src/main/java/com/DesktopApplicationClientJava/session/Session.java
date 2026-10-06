package com.DesktopApplicationClientJava.session;

import com.DesktopApplicationClientJava.entities.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Session {

  private User currentUser;
  private String accessToken;
  private String refreshToken;

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
