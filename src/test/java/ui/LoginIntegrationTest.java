package ui;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.DesktopApplicationClientJava.Api.ApiClient;
import com.DesktopApplicationClientJava.services.AuthService;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.services.impl.AuthServiceImpl;
import com.DesktopApplicationClientJava.session.Session;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Integration test for the login flow: real {@link AuthService} with a real {@link ApiClient}, no
 * mocks. Requires a running backend at {@code localhost:8080}; otherwise tests are aborted.
 *
 * <p>Excluded from the default build and CI (see surefire {@code excludedGroups} in pom.xml). Run
 * explicitly: {@code mvn test -Dgroups=integration -DexcludedGroups=}
 */
@Tag("integration")
class LoginIntegrationTest {

  private AuthService authService;

  @BeforeEach
  void setUp() {
    assumeTrue(isBackendUp(), "Backend is not running at localhost:8080, skipping");
    Session session = new Session();
    authService = new AuthServiceImpl(new ApiClient(session), session);
  }

  @Test
  void invalidCredentialsAreRejectedByRealApi() {
    assertThrows(
        ServiceException.class,
        () -> authService.login("no-such-user@example.com", "wrong-password-123"));
  }

  private static boolean isBackendUp() {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress("localhost", 8080), 2000);
      return true;
    } catch (IOException e) {
      return false;
    }
  }
}
