package ui.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.DesktopApplicationClientJava.Api.ApiClient;
import com.DesktopApplicationClientJava.controllers.ResourceFormController;
import com.DesktopApplicationClientJava.entities.Resource;
import com.DesktopApplicationClientJava.navigation.Navigator;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.services.ServiceRegistry;
import com.DesktopApplicationClientJava.services.impl.AuthServiceImpl;
import com.DesktopApplicationClientJava.services.impl.ResourceServiceImpl;
import com.DesktopApplicationClientJava.session.Session;
import com.DesktopApplicationClientJava.utils.ControllerWiring;
import java.util.UUID;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import ui.FxTestBase;

/**
 * Integration tests for the resource create/edit flow: real services with a real ApiClient against
 * a live backend, no mocks (except Navigator).
 *
 * <p>UI interaction (fill/save/navigate/error display/prefill) is covered by {@code
 * ResourceFormControllerTest} with mocked services; these tests assert the backend contract only,
 * plus one UI test for the authenticated-failure path that mocks cannot produce.
 *
 * <p>Requires a running backend at {@code localhost:8080} and {@code IT_ADMIN_PASSWORD} in {@code
 * .env}; otherwise tests abort. Excluded from the default build and CI (see surefire {@code
 * excludedGroups} in pom.xml). Run explicitly: {@code mvn test -Dgroups=integration
 * -DexcludedGroups= -Dtest=ResourceFormIntegrationTest}.
 */
class ResourceFormIntegrationTest extends IntegrationTestCase {

  private final FxRobot robot = new FxRobot();
  private Navigator navigator;
  private Stage stage;
  private Session session;
  private ServiceRegistry services;

  @BeforeAll
  static void initToolkit() throws Exception {
    FxTestBase.startSharedToolkit();
  }

  @AfterAll
  static void cleanupToolkit() throws Exception {
    FxTestBase.stopSharedToolkit(new FxRobot());
  }

  @BeforeEach
  void resetScene() throws Exception {
    FxToolkit.setupStage(
        stage -> {
          try {
            start(stage);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  void start(Stage stage) {
    this.stage = stage;
    this.navigator = mock(Navigator.class);
    stage.setScene(new Scene(new StackPane()));
    stage.show();
  }

  @Override
  @BeforeEach
  void setUpServices() {
    assumeTrue(isBackendUp(), "Backend is not running at localhost:8080, skipping");
    session = new Session();
    ApiClient apiClient = new ApiClient(session);
    authService = new AuthServiceImpl(apiClient, session);
    resourceService = new ResourceServiceImpl(apiClient);
    services = new ServiceRegistry(apiClient, session);
  }

  @Test
  void createValidRoundTrip() {
    loginAsAdmin();
    String title = "IT ResourceForm " + UUID.randomUUID();
    Resource created = resourceService.create(title, "created by ResourceFormIntegrationTest");
    try {
      Resource fetched = resourceService.getById(created.getUuid());
      assertEquals(created.getUuid(), fetched.getUuid());
      assertEquals(title, fetched.getTitle());
    } finally {
      deleteQuietly(created.getUuid());
    }
  }

  @Test
  void createNullDescriptionRoundTrip() {
    loginAsAdmin();
    String title = "IT ResourceForm " + UUID.randomUUID();
    Resource created = resourceService.create(title, null);
    try {
      assertNull(resourceService.getById(created.getUuid()).getDescription());
    } finally {
      deleteQuietly(created.getUuid());
    }
  }

  @Test
  void updateValidRoundTrip() {
    loginAsAdmin();
    String updatedTitle = "IT ResourceForm updated " + UUID.randomUUID();
    Resource created = resourceService.create("IT ResourceForm " + UUID.randomUUID(), null);
    try {
      resourceService.update(created.getUuid(), updatedTitle, "updated description");

      Resource fetched = resourceService.getById(created.getUuid());
      assertEquals(updatedTitle, fetched.getTitle());
      assertEquals("updated description", fetched.getDescription());
    } finally {
      deleteQuietly(created.getUuid());
    }
  }

  @Test
  void invalidTitlesAreRejectedWith400() {
    loginAsAdmin();

    assertStatus(400, () -> resourceService.create("", "blank title"));
    assertStatus(400, () -> resourceService.create("ab", "short title"));
    assertStatus(400, () -> resourceService.create("a".repeat(256), "long title"));
  }

  @Test
  void emptyDescriptionIsRejectedWith400() {
    loginAsAdmin();

    // The backend requires @Size(min = 1) on description: this is why the UI sends null
    // for blank input instead of "".
    assertStatus(400, () -> resourceService.create("IT ResourceForm " + UUID.randomUUID(), ""));
  }

  @Test
  void updateUnknownIdReturns404() {
    loginAsAdmin();

    assertStatus(404, () -> resourceService.update(UUID.randomUUID(), "IT ResourceForm", null));
  }

  @Test
  void saveWithInvalidTokensShowsErrorAndDoesNotNavigate() {
    // Garbage tokens (not a fresh session) force a real HTTP 401 round-trip instead of the
    // client's offline "no refresh token" shortcut, proving server-side auth enforcement.
    Session poisoned = new Session();
    poisoned.setAccessToken("invalid");
    poisoned.setRefreshToken("invalid");
    ServiceRegistry poisonedServices = new ServiceRegistry(new ApiClient(poisoned), poisoned);
    loadFormPage(robot, poisoned, poisonedServices);

    robot.interact(
        () -> {
          robot.lookup("#titleField").queryAs(TextField.class).setText("IT ResourceForm");
          robot.lookup("#descriptionArea").queryAs(TextArea.class).setText("description");
          // Fire the action directly: synthetic clicks are unreliable under headless Monocle
          // (silently missed, see ResourceListIntegrationTest helpers).
          robot.lookup("#saveButton").queryAs(Button.class).getOnAction().handle(new ActionEvent());
        });

    Label error = robot.lookup("#errorLabel").queryAs(Label.class);
    assertTrue(error.isVisible());
    assertFalse(error.getText().isBlank());
    verify(navigator, never()).goTo(anyString());
  }

  private void loginAsAdmin() {
    assumeTrue(
        adminPassword != null && !adminPassword.isBlank(),
        "IT_ADMIN_PASSWORD is not set, skipping");
    authService.login(adminEmail, adminPassword);
  }

  private void loadFormPage(FxRobot robot, Session pageSession, ServiceRegistry pageServices) {
    robot.interact(
        () -> {
          try {
            ResourceFormController controller = new ResourceFormController();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ResourceForm.fxml"));
            loader.setControllerFactory(
                clazz -> {
                  ControllerWiring.wire(controller, navigator, pageSession, pageServices);
                  return controller;
                });
            Parent root = loader.load();
            stage.setScene(new Scene(root));
            stage.show();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  private static void assertStatus(int expected, Runnable call) {
    try {
      call.run();
    } catch (ServiceException e) {
      assertEquals(expected, e.getStatus(), "unexpected status: " + e.getMessage());
      return;
    }
    throw new AssertionError("Expected ServiceException with status " + expected);
  }

  private void deleteQuietly(UUID id) {
    try {
      resourceService.delete(id);
    } catch (Exception ignored) {
      // Best-effort cleanup, never fail the test.
    }
  }
}
