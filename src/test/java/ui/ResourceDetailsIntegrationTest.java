package ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.DesktopApplicationClientJava.Api.ApiClient;
import com.DesktopApplicationClientJava.entities.Resource;
import com.DesktopApplicationClientJava.services.AuthService;
import com.DesktopApplicationClientJava.services.ResourceService;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.services.impl.AuthServiceImpl;
import com.DesktopApplicationClientJava.services.impl.ResourceServiceImpl;
import com.DesktopApplicationClientJava.session.Session;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Integration test for the resource details flow: real services with a real {@link ApiClient}, no
 * mocks. Requires a running backend at {@code localhost:8080}; otherwise tests are aborted.
 *
 * <p>Excluded from the default build and CI (see surefire {@code excludedGroups} in pom.xml). Run
 * explicitly: {@code mvn test -Dgroups=integration -DexcludedGroups=}
 */
@Tag("integration")
class ResourceDetailsIntegrationTest {

  // Seed admin of the local dev backend. Password only from the environment (see
  // .env.example); the test is skipped when it is not set. No secrets in source.
  private static final String ADMIN_EMAIL =
      System.getenv().getOrDefault("IT_ADMIN_EMAIL", "admin@example.com");
  private static final String ADMIN_PASSWORD = System.getenv("IT_ADMIN_PASSWORD");

  private AuthService authService;
  private ResourceService resourceService;

  @BeforeEach
  void setUp() {
    assumeTrue(isBackendUp(), "Backend is not running at localhost:8080, skipping");
    Session session = new Session();
    ApiClient apiClient = new ApiClient(session);
    authService = new AuthServiceImpl(apiClient, session);
    resourceService = new ResourceServiceImpl(apiClient);
  }

  @Test
  void unknownIdThrowsServiceException() {
    assertThrows(ServiceException.class, () -> resourceService.getById(UUID.randomUUID()));
  }

  @Test
  void createdResourceRoundTrip() {
    assumeTrue(
        ADMIN_PASSWORD != null && !ADMIN_PASSWORD.isBlank(),
        "IT_ADMIN_PASSWORD is not set, skipping");
    authService.login(ADMIN_EMAIL, ADMIN_PASSWORD);
    String title = "IT ResourceDetails " + UUID.randomUUID();
    Resource created = resourceService.create(title, "created by integration test");
    try {
      Resource fetched = resourceService.getById(created.getUuid());
      assertEquals(created.getUuid(), fetched.getUuid());
      assertEquals(title, fetched.getTitle());
      assertNotNull(fetched.getFiles());
    } finally {
      resourceService.delete(created.getUuid());
    }
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
