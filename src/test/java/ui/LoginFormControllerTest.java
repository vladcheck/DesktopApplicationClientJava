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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

@ExtendWith(ApplicationExtension.class)
class LoginFormControllerTest {

  private static final String VALID_EMAIL = "user@example.com";
  private static final String VALID_PASSWORD = "secret123";

  private AuthService authService;
  private Navigator navigator;

  @Start
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
  void emptyFieldsShowErrorAndDoNotCallApi(FxRobot robot) {
    robot.clickOn("#submitButton");

    Label error = robot.lookup("#errorLabel").queryAs(Label.class);
    assertTrue(error.isVisible());
    assertEquals("Заполните все поля", error.getText());
    verify(authService, never()).login(anyString(), anyString());
    verify(navigator, never()).goTo(anyString());
  }

  @Test
  void blankPasswordShowsErrorAndDoesNotCallApi(FxRobot robot) {
    robot.clickOn("#emailField").write(VALID_EMAIL);
    robot.clickOn("#submitButton");

    Label error = robot.lookup("#errorLabel").queryAs(Label.class);
    assertTrue(error.isVisible());
    assertEquals("Заполните все поля", error.getText());
    verify(authService, never()).login(anyString(), anyString());
    verify(navigator, never()).goTo(anyString());
  }

  @Test
  void invalidEmailShowsErrorAndDoesNotCallApi(FxRobot robot) {
    robot.clickOn("#emailField").write("not-an-email");
    robot.clickOn("#passwordField").write(VALID_PASSWORD);
    robot.clickOn("#submitButton");

    Label error = robot.lookup("#errorLabel").queryAs(Label.class);
    assertTrue(error.isVisible());
    assertEquals("Неверный формат", error.getText());
    verify(authService, never()).login(anyString(), anyString());
    verify(navigator, never()).goTo(anyString());
  }

  @Test
  void successfulLoginNavigatesToResourceList(FxRobot robot) {
    User user = new User(UUID.randomUUID(), VALID_EMAIL, Role.USER, "Имя", "Фамилия", true);
    when(authService.login(VALID_EMAIL, VALID_PASSWORD)).thenReturn(user);

    robot.clickOn("#emailField").write(VALID_EMAIL);
    robot.clickOn("#passwordField").write(VALID_PASSWORD);
    robot.clickOn("#submitButton");

    verify(authService).login(VALID_EMAIL, VALID_PASSWORD);
    verify(navigator).goTo("/fxml/ResourceList.fxml");
    Label error = robot.lookup("#errorLabel").queryAs(Label.class);
    assertFalse(error.isVisible());
  }

  @Test
  void failedLoginShowsServerErrorAndDoesNotNavigate(FxRobot robot) {
    when(authService.login(VALID_EMAIL, VALID_PASSWORD))
        .thenThrow(new ServiceException("Неверный логин или пароль"));

    robot.clickOn("#emailField").write(VALID_EMAIL);
    robot.clickOn("#passwordField").write(VALID_PASSWORD);
    robot.clickOn("#submitButton");

    Label error = robot.lookup("#errorLabel").queryAs(Label.class);
    assertTrue(error.isVisible());
    assertEquals("Неверный логин или пароль", error.getText());
    verify(navigator, never()).goTo(anyString());
  }
}
