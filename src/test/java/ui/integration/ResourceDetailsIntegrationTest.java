package ui.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.DesktopApplicationClientJava.entities.Resource;
import com.DesktopApplicationClientJava.services.ServiceException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Integration test for the resource details flow: real services with a real ApiClient, no mocks.
 * Requires a running backend at {@code localhost:8080}; otherwise tests are aborted.
 *
 * <p>Excluded from the default build and CI (see surefire {@code excludedGroups} in pom.xml). Run
 * explicitly: {@code mvn test -Dgroups=integration -DexcludedGroups=} ({@code @Tag} inherited from
 * IntegrationTestCase).
 */
class ResourceDetailsIntegrationTest extends IntegrationTestCase {
  @Test
  void unknownIdThrowsServiceException() {
    assertThrows(ServiceException.class, () -> resourceService.getById(UUID.randomUUID()));
  }

  @Test
  void createdResourceRoundTrip() {
    assumeTrue(
        adminPassword != null && !adminPassword.isBlank(),
        "IT_ADMIN_PASSWORD is not set, skipping");
    authService.login(adminEmail, adminPassword);
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
}
