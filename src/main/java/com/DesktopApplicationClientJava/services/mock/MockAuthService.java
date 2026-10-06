package com.DesktopApplicationClientJava.services.mock;

import com.DesktopApplicationClientJava.entities.Role;
import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.services.AuthService;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.session.Session;
import java.util.UUID;

/**
 * In-memory {@link AuthService}. Accepts any non-blank credentials; the role comes from the seeded
 * user when the email matches, otherwise from the email prefix ({@code admin*} → ADMIN, {@code
 * moder*} → MODER, anything else → USER).
 */
public class MockAuthService implements AuthService, MockService {

  private final MockStore store;
  private final Session session;

  public MockAuthService(MockStore store, Session session) {
    this.store = store;
    this.session = session;
  }

  @Override
  public User login(String email, String password) {
    if (email == null || email.isBlank() || password == null || password.isBlank()) {
      throw new ServiceException("Неверный логин или пароль", 401);
    }
    User user = findByEmail(email);
    if (user == null) {
      user = new User(UUID.randomUUID(), email.trim(), roleByEmail(email), "Мок", "", true);
    }
    session.setCurrentUser(user);
    return user;
  }

  @Override
  public void logout() {
    session.clear();
  }

  @Override
  public User getCurrentUser() {
    return session.getCurrentUser();
  }

  @Override
  public boolean hasRole(Role required) {
    User current = session.getCurrentUser();
    return current != null
        && current.getRole() != null
        && level(current.getRole()) >= level(required);
  }

  @Override
  public boolean isAdmin() {
    return hasRole(Role.ADMIN);
  }

  @Override
  public boolean isModerator() {
    return hasRole(Role.MODER);
  }

  private User findByEmail(String email) {
    String normalized = email.trim();
    return store.getUsers().values().stream()
        .filter(user -> user.getEmail().equalsIgnoreCase(normalized))
        .findFirst()
        .orElse(null);
  }

  private static Role roleByEmail(String email) {
    String local = email.trim().toLowerCase();
    int at = local.indexOf('@');
    String prefix = at < 0 ? local : local.substring(0, at);
    if (prefix.startsWith("admin")) {
      return Role.ADMIN;
    }
    if (prefix.startsWith("moder")) {
      return Role.MODER;
    }
    return Role.USER;
  }

  private static int level(Role role) {
    return switch (role) {
      case USER -> 0;
      case MODER -> 1;
      case ADMIN -> 2;
    };
  }
}
