package services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.DesktopApplicationClientJava.Api.ApiClient;
import com.DesktopApplicationClientJava.entities.Role;
import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.services.AuthService;
import com.DesktopApplicationClientJava.services.impl.AuthServiceImpl;
import com.DesktopApplicationClientJava.session.Session;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthServiceTest {

  private Session session;
  private AuthService authService;

  @BeforeEach
  void setUp() {
    session = new Session();
    authService = new AuthServiceImpl(mock(ApiClient.class), session);
  }

  @Test
  void guestHasNoRoles() {
    assertFalse(authService.hasRole(Role.USER));
    assertFalse(authService.hasRole(Role.MODER));
    assertFalse(authService.hasRole(Role.ADMIN));
  }

  @Test
  void userHasOnlyUserRole() {
    loginAs(Role.USER);

    assertTrue(authService.hasRole(Role.USER));
    assertFalse(authService.hasRole(Role.MODER));
    assertFalse(authService.hasRole(Role.ADMIN));
  }

  @Test
  void moderHasUserAndModerRolesButIsNotAdmin() {
    loginAs(Role.MODER);

    assertTrue(authService.hasRole(Role.USER));
    assertTrue(authService.hasRole(Role.MODER));
    assertFalse(authService.hasRole(Role.ADMIN));
    assertTrue(authService.isModerator());
    assertFalse(authService.isAdmin());
  }

  @Test
  void adminHasAllRoles() {
    loginAs(Role.ADMIN);

    assertTrue(authService.hasRole(Role.USER));
    assertTrue(authService.hasRole(Role.MODER));
    assertTrue(authService.hasRole(Role.ADMIN));
    assertTrue(authService.isModerator());
    assertTrue(authService.isAdmin());
  }

  private void loginAs(Role role) {
    session.setCurrentUser(
        new User(UUID.randomUUID(), "user@example.com", role, "Имя", "Фамилия", true));
  }
}
