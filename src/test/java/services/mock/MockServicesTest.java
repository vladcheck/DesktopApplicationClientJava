package services.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.DesktopApplicationClientJava.entities.FileInfo;
import com.DesktopApplicationClientJava.entities.Resource;
import com.DesktopApplicationClientJava.entities.Role;
import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.services.filter.ResourceFilter;
import com.DesktopApplicationClientJava.services.filter.UserFilter;
import com.DesktopApplicationClientJava.services.mock.MockAuthService;
import com.DesktopApplicationClientJava.services.mock.MockFileService;
import com.DesktopApplicationClientJava.services.mock.MockResourceService;
import com.DesktopApplicationClientJava.services.mock.MockStore;
import com.DesktopApplicationClientJava.services.mock.MockUsersService;
import com.DesktopApplicationClientJava.session.Session;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MockServicesTest {

  private Session session;
  private MockAuthService authService;
  private MockResourceService resourceService;
  private MockFileService fileService;
  private MockUsersService usersService;

  @BeforeEach
  void setUp() {
    MockStore store = new MockStore();
    session = new Session();
    authService = new MockAuthService(store, session);
    resourceService = new MockResourceService(store);
    fileService = new MockFileService(store);
    usersService = new MockUsersService(store);
  }

  @Test
  void seededAdminKeepsAdminRole() {
    User user = authService.login("admin@example.com", "whatever");

    assertEquals(Role.ADMIN, user.getRole());
    assertTrue(authService.isAdmin());
    assertTrue(authService.isModerator());
  }

  @Test
  void unknownEmailGetsRoleByPrefix() {
    assertEquals(Role.MODER, authService.login("moder-ivan@example.com", "x").getRole());
    assertEquals(Role.USER, authService.login("someone@example.com", "x").getRole());
  }

  @Test
  void blankCredentialsAreRejected() {
    assertThrows(ServiceException.class, () -> authService.login("", "x"));
    assertThrows(ServiceException.class, () -> authService.login("a@b.c", ""));
  }

  @Test
  void logoutClearsSession() {
    authService.login("user@example.com", "x");
    authService.logout();

    assertNull(authService.getCurrentUser());
  }

  @Test
  void seededResourceHasFiles() {
    List<Resource> found = resourceService.search(new ResourceFilter("отчёт", null), 0, 10);

    assertEquals(1, found.size());
    assertEquals(2, found.get(0).getFiles().size());
  }

  @Test
  void unknownResourceThrows404() {
    ServiceException e =
        assertThrows(ServiceException.class, () -> resourceService.getById(UUID.randomUUID()));
    assertEquals(404, e.getStatus());
  }

  @Test
  void createAndDeleteRoundTrip() {
    Resource created = resourceService.create("Временный", "удалится");
    resourceService.delete(created.getUuid());

    assertThrows(ServiceException.class, () -> resourceService.getById(created.getUuid()));
  }

  @Test
  void downloadWritesFileWithOriginalName(@TempDir Path tempDir) throws Exception {
    Resource resource = resourceService.search(ResourceFilter.empty(), 0, 10).get(0);
    FileInfo file = resource.getFiles().get(0);

    Path saved = fileService.downloadFile(resource.getUuid(), file.getUuid(), tempDir);

    assertEquals(file.getName(), saved.getFileName().toString());
    assertTrue(Files.size(saved) > 0);
  }

  @Test
  void downloadUnknownFileThrows404(@TempDir Path tempDir) {
    Resource resource = resourceService.search(ResourceFilter.empty(), 0, 10).get(0);

    ServiceException e =
        assertThrows(
            ServiceException.class,
            () -> fileService.downloadFile(resource.getUuid(), UUID.randomUUID(), tempDir));
    assertEquals(404, e.getStatus());
  }

  @Test
  void usersSearchFindsSeededAdmin() {
    List<User> found = usersService.search(new UserFilter("admin@", null, null, null, null), 0, 10);

    assertEquals(1, found.size());
    assertEquals(Role.ADMIN, found.get(0).getRole());
    assertNotNull(usersService.getById(found.get(0).getUuid()));
  }
}
