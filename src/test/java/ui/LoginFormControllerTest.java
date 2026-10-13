package ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.DesktopApplicationClientJava.entities.Role;
import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.navigation.Navigator;
import com.DesktopApplicationClientJava.services.AuthService;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.services.ServiceRegistry;
import com.DesktopApplicationClientJava.session.Session;
import com.DesktopApplicationClientJava.utils.ControllerWiring;
import java.util.UUID;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxToolkit;

class LoginFormControllerTest extends FxTestBase {

  private static final String VALID_EMAIL = "user@example.com";
  private static final String VALID_PASSWORD = "secret123";

  private AuthService authService;
  private Navigator navigator;

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

  void start(Stage stage) throws Exception {
    authService = mock(AuthService.class);
    ServiceRegistry services = mock(ServiceRegistry.class);
    when(services.getAuthService()).thenReturn(authService);
    navigator = mock(Navigator.class);

    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginForm.fxml"));
    loader.setControllerFactory(
        clazz -> {
          try {
            Object controller = clazz.getDeclaredConstructor().newInstance();
            ControllerWiring.wire(controller, navigator, new Session(), services);
            return controller;
          } catch (Exception e) {
            throw new RuntimeException("Cannot create controller: " + clazz, e);
          }
        });
    Parent root = loader.load();
    stage.setScene(new Scene(root));
    stage.show();
  }

  @Test
  void emptyFieldsShowErrorAndDoNotCallApi() {
    robot.clickOn("#submitButton");

    Label error = robot.lookup("#errorLabel").queryAs(Label.class);
    assertTrue(error.isVisible());
    assertEquals("Заполните все поля", error.getText());
    verify(authService, never()).login(anyString(), anyString());
    verify(navigator, never()).goTo(anyString());
  }

  @Test
  void blankPasswordShowsErrorAndDoesNotCallApi() {
    fillText(robot, "#emailField", VALID_EMAIL);
    robot.clickOn("#submitButton");

    Label error = robot.lookup("#errorLabel").queryAs(Label.class);
    assertTrue(error.isVisible());
    assertEquals("Заполните все поля", error.getText());
    verify(authService, never()).login(anyString(), anyString());
    verify(navigator, never()).goTo(anyString());
  }

  @Test
  void invalidEmailShowsErrorAndDoesNotCallApi() {
    fillText(robot, "#emailField", "not-an-email");
    fillText(robot, "#passwordField", VALID_PASSWORD);
    robot.clickOn("#submitButton");

    Label error = robot.lookup("#errorLabel").queryAs(Label.class);
    assertTrue(error.isVisible());
    assertEquals("Неверный формат", error.getText());
    verify(authService, never()).login(anyString(), anyString());
    verify(navigator, never()).goTo(anyString());
  }

  @Test
  void successfulLoginNavigatesToResourceList() {
    User user = new User(UUID.randomUUID(), VALID_EMAIL, Role.USER, "Имя", "Фамилия", true);
    when(authService.login(VALID_EMAIL, VALID_PASSWORD)).thenReturn(user);

    fillText(robot, "#emailField", VALID_EMAIL);
    fillText(robot, "#passwordField", VALID_PASSWORD);
    robot.clickOn("#submitButton");

    verify(authService).login(VALID_EMAIL, VALID_PASSWORD);
    verify(navigator).goTo("/fxml/ResourceList.fxml");
    Label error = robot.lookup("#errorLabel").queryAs(Label.class);
    assertFalse(error.isVisible());
  }

  @Test
  void failedLoginShowsServerErrorAndDoesNotNavigate() {
    when(authService.login(VALID_EMAIL, VALID_PASSWORD))
        .thenThrow(new ServiceException("Неверный логин или пароль"));

    fillText(robot, "#emailField", VALID_EMAIL);
    fillText(robot, "#passwordField", VALID_PASSWORD);
    robot.clickOn("#submitButton");

    Label error = robot.lookup("#errorLabel").queryAs(Label.class);
    assertTrue(error.isVisible());
    assertEquals("Неверный логин или пароль", error.getText());
    verify(navigator, never()).goTo(anyString());
  }
}
