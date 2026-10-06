package ui.integration;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.DesktopApplicationClientJava.services.ServiceException;
import org.junit.jupiter.api.Test;

/**
 * Integration test for the login flow: real AuthService with a real ApiClient, no mocks. Requires a
 * running backend at {@code localhost:8080}; otherwise tests are aborted.
 *
 * <p>Excluded from the default build and CI (see surefire {@code excludedGroups} in pom.xml). Run
 * explicitly: {@code mvn test -Dgroups=integration -DexcludedGroups=} ({@code @Tag} inherited from
 * IntegrationTestCase).
 */
class LoginIntegrationTest extends IntegrationTestCase {

  @Test
  void invalidCredentialsAreRejectedByRealApi() {
    assertThrows(
        ServiceException.class,
        () -> authService.login("no-such-user@example.com", "wrong-password-123"));
  }
}
