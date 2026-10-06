package ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.DesktopApplicationClientJava.controllers.ResourceDetailsController;
import com.DesktopApplicationClientJava.entities.FileInfo;
import com.DesktopApplicationClientJava.navigation.Navigator;
import com.DesktopApplicationClientJava.services.ServiceRegistry;
import com.DesktopApplicationClientJava.session.Session;
import com.DesktopApplicationClientJava.utils.ControllerWiring;
import java.util.UUID;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

/**
 * Mock-backed UI flow: real pages wired to real in-memory mock services (no Mockito on services, no
 * backend). Proves that MOCK_DATA mode renders actual data.
 */
@ExtendWith(ApplicationExtension.class)
class MockFlowTest {

  private static final UUID SEEDED_RESOURCE_ID =
      UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

  private ResourceDetailsController controller;

  @Start
  void start(Stage stage) throws Exception {
    Navigator navigator = mockNavigator();
    ServiceRegistry services = ServiceRegistry.mock(new Session());

    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ResourceDetails.fxml"));
    loader.setControllerFactory(
        clazz -> {
          try {
            Object created = clazz.getDeclaredConstructor().newInstance();
            ControllerWiring.wire(created, navigator, new Session(), services);
            return created;
          } catch (Exception e) {
            throw new RuntimeException("Cannot create controller: " + clazz, e);
          }
        });
    Parent root = loader.load();
    controller = loader.getController();
    stage.setScene(new Scene(root));
    stage.show();
  }

  @Test
  void mockLoginPutsUserIntoSession() {
    Session session = new Session();
    ServiceRegistry registry = ServiceRegistry.mock(session);

    registry.getAuthService().login("admin@example.com", "any-password");

    assertEquals("admin@example.com", session.getCurrentUser().getEmail());
  }

  @Test
  @SuppressWarnings("unchecked")
  void detailsPageRendersSeededMockResource(FxRobot robot) {
    robot.interact(
        () -> {
          controller.setResourceId(SEEDED_RESOURCE_ID);
          controller.load();
        });

    assertEquals("Годовой отчёт (мок)", robot.lookup("#titleLabel").queryAs(Label.class).getText());
    TableView<FileInfo> table = robot.lookup("#filesTable").queryAs(TableView.class);
    assertEquals(2, table.getItems().size());
    assertFalse(robot.lookup("#errorLabel").queryAs(Label.class).isVisible());
  }

  @Test
  @SuppressWarnings("unchecked")
  void detailsPageAutoSelectsFirstSeedWithoutId(FxRobot robot) {
    robot.interact(() -> controller.load());

    assertEquals("Годовой отчёт (мок)", robot.lookup("#titleLabel").queryAs(Label.class).getText());
    TableView<FileInfo> table = robot.lookup("#filesTable").queryAs(TableView.class);
    assertEquals(2, table.getItems().size());
  }

  private static Navigator mockNavigator() {
    return org.mockito.Mockito.mock(Navigator.class);
  }
}
