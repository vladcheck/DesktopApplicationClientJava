package com.DesktopApplicationClientJava.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.DesktopApplicationClientJava.entities.Role;
import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.navigation.Navigator;
import com.DesktopApplicationClientJava.services.AuthService;
import com.DesktopApplicationClientJava.services.ResourceService;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.services.ServiceRegistry;
import com.DesktopApplicationClientJava.services.UsersService;
import com.DesktopApplicationClientJava.session.Session;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.testfx.api.FxToolkit;
import org.testfx.util.WaitForAsyncUtils;

@Epic("Безопасность")
@Feature("Перехват 401/403")
@DisplayName("Перехват 401/403 ведёт на вход с очищенной сессией")
class AuthInterceptorTest extends FxTestBase {

  private static final User ADMIN =
      new User(UUID.randomUUID(), "admin@example.com", Role.ADMIN, "Имя", "Фамилия", true);

  private AuthService authService;
  private ResourceService resourceService;
  private UsersService usersService;
  private ServiceRegistry services;
  private Session session;
  private Navigator navigator;
  private Stage stage;

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
    authService = Mockito.mock(AuthService.class);
    resourceService = Mockito.mock(ResourceService.class);
    usersService = Mockito.mock(UsersService.class);
    services = Mockito.mock(ServiceRegistry.class);
    when(services.getAuthService()).thenReturn(authService);
    when(services.getResourceService()).thenReturn(resourceService);
    when(services.getUsersService()).thenReturn(usersService);
    when(authService.getCurrentUser()).thenReturn(ADMIN);
    when(authService.isAdmin()).thenReturn(true);
    when(authService.isModerator()).thenReturn(false);
    when(resourceService.search(any(), anyLong(), anyLong(), any(), any())).thenReturn(List.of());
    session = new Session();
    session.setCurrentUser(ADMIN);
    session.setAccessToken("access");
    session.setRefreshToken("refresh");
    this.stage = stage;
    navigator = new Navigator(stage, session, services);
    robot.interact(
        () -> {
          stage.setScene(new Scene(new StackPane()));
          stage.show();
        });
  }

  @Test
  @Story("Синхронный путь")
  @DisplayName("Синхронная 401 ведёт на вход с очищенной сессией")
  void sync401RedirectsToLoginWithClearedSession() {
    when(usersService.create(any())).thenThrow(new ServiceException("Сессия истекла", 401));
    robot.interact(() -> navigator.goTo("/fxml/UserCreateForm.fxml"));
    waitForNode("#submitButton");
    robot.interact(
        () -> {
          robot
              .lookup("#emailField")
              .queryAs(javafx.scene.control.TextField.class)
              .setText("new@example.com");
          robot
              .lookup("#passwordField")
              .queryAs(javafx.scene.control.PasswordField.class)
              .setText("secret123");
          robot
              .lookup("#firstNameField")
              .queryAs(javafx.scene.control.TextField.class)
              .setText("Имя");
          robot
              .lookup("#lastNameField")
              .queryAs(javafx.scene.control.TextField.class)
              .setText("Фамилия");
          robot
              .lookup("#rolesChoiceBox")
              .queryAs(javafx.scene.control.ChoiceBox.class)
              .setValue("USER");
          robot
              .lookup("#submitButton")
              .queryAs(Button.class)
              .getOnAction()
              .handle(new ActionEvent());
        });
    waitForLoginForm();

    assertTrue(onLoginForm(), "401 должна вести на форму входа");
    assertNull(session.getCurrentUser(), "сессия должна быть очищена");
    assertNull(session.getAccessToken(), "токен должен быть очищен");
    assertNull(session.getRefreshToken(), "refresh-токен должен быть очищен");
  }

  @Test
  @Story("Асинхронный путь")
  @DisplayName("Асинхронная 403 ведёт на вход с очищенной сессией")
  void async403RedirectsToLoginWithClearedSession() {
    when(resourceService.search(any(), anyLong(), anyLong(), anyString(), anyString()))
        .thenThrow(new ServiceException("Доступ запрещён", 403));
    robot.interact(() -> navigator.goTo("/fxml/ResourceList.fxml"));
    waitForNode("#submitButton");

    assertTrue(onLoginForm(), "403 должна вести на форму входа");
    assertNull(session.getCurrentUser(), "сессия должна быть очищена");
    assertNull(session.getAccessToken(), "токен должен быть очищен");
    assertNull(session.getRefreshToken(), "refresh-токен должен быть очищен");
  }

  @Test
  @Story("Форма входа")
  @DisplayName("Неверный пароль остаётся на форме входа без выхода")
  void wrongPasswordStaysOnLoginForm() {
    robot.interact(session::clear);
    robot.interact(() -> navigator.goTo("/fxml/LoginForm.fxml"));
    waitForNode("#submitButton");
    when(authService.login(anyString(), anyString()))
        .thenThrow(new ServiceException("Неверный логин или пароль", 401));

    robot.interact(
        () -> {
          robot
              .lookup("#emailField")
              .queryAs(javafx.scene.control.TextField.class)
              .setText("user@example.com");
          robot
              .lookup("#passwordField")
              .queryAs(javafx.scene.control.PasswordField.class)
              .setText("wrong");
          robot
              .lookup("#submitButton")
              .queryAs(Button.class)
              .getOnAction()
              .handle(new ActionEvent());
        });
    waitForLoginError("Неверный логин или пароль");

    assertTrue(onLoginForm(), "неверный пароль должен оставить на форме входа");
    assertFalse(present("#resourceTable"), "перехода на список ресурсов быть не должно");
    assertNull(session.getCurrentUser(), "сессия должна остаться пустой");
  }

  @Test
  @Story("Выход")
  @DisplayName("Выход съедает ошибку сервиса без мелькания и ведёт на вход")
  void logoutSwallowsErrorWithoutFlicker() {
    robot.interact(() -> navigator.goTo("/fxml/ResourceList.fxml"));
    waitForNode("#resourceTable");
    doThrow(new ServiceException("Сессия уже истекла", 403)).when(authService).logout();

    robot.interact(
        () ->
            robot
                .lookup("#logoutButton")
                .queryAs(Button.class)
                .getOnAction()
                .handle(new ActionEvent()));
    waitForNode("#submitButton");

    assertTrue(onLoginForm(), "выход должен вести на форму входа");
    assertNull(session.getCurrentUser(), "сессия должна быть очищена");
    Label loginError = robot.lookup("#errorLabel").queryAs(Label.class);
    assertFalse(loginError.isVisible(), "ошибка выхода не должна мелькать на форме входа");
    assertEquals("", loginError.getText(), "текст ошибки выхода должен быть пуст");
  }

  @Test
  @Story("Параллельные отказы")
  @DisplayName("Три параллельные 403 завершаются однократным входом без гонки")
  void parallel403EndsOnLoginOnce() throws Exception {
    when(resourceService.search(any(), anyLong(), anyLong(), anyString(), anyString()))
        .thenThrow(new ServiceException("Доступ запрещён", 403));
    robot.interact(
        () -> {
          navigator.goToLoginOnce();
          navigator.goToLoginOnce();
          navigator.goToLoginOnce();
        });
    waitForNode("#submitButton");

    assertTrue(onLoginForm(), "параллельные 403 должны завершиться на форме входа");
  }

  private void waitForNode(String query) {
    try {
      WaitForAsyncUtils.waitFor(
          10,
          TimeUnit.SECONDS,
          () -> {
            try {
              robot.lookup(query).query();
              return true;
            } catch (Exception e) {
              return false;
            }
          });
    } catch (TimeoutException e) {
      throw new AssertionError("Не дождались узла " + query, e);
    }
  }

  private void waitForLoginForm() {
    try {
      WaitForAsyncUtils.waitFor(
          10,
          TimeUnit.SECONDS,
          () -> {
            try {
              robot.lookup("#submitButton").query();
              return !present("#firstNameField") && present("#passwordField");
            } catch (Exception e) {
              return false;
            }
          });
    } catch (TimeoutException e) {
      throw new AssertionError("Не дождались формы входа", e);
    }
  }

  private void waitForLoginError(String text) {
    try {
      WaitForAsyncUtils.waitFor(
          10,
          TimeUnit.SECONDS,
          () -> {
            try {
              Label error = robot.lookup("#errorLabel").queryAs(Label.class);
              return error.isVisible() && text.equals(error.getText());
            } catch (Exception e) {
              return false;
            }
          });
    } catch (TimeoutException e) {
      throw new AssertionError("Не дождались ошибки входа «" + text + "»", e);
    }
  }

  private boolean present(String query) {
    try {
      robot.lookup(query).query();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private boolean onLoginForm() {
    return present("#submitButton")
        && present("#passwordField")
        && !present("#firstNameField")
        && !present("#resourceTable")
        && !present("#usersTable");
  }
}
