package com.DesktopApplicationClientJava.controllers;

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
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.util.UUID;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Epic("Аутентификация")
@Feature("№1 Вход")
@Tag("role:guest")
@DisplayName("Форма входа")
class LoginFormControllerTest {

  private static final String VALID_EMAIL = "user@example.com";
  private static final String VALID_PASSWORD = "secret123";

  private AuthService authService;
  private Navigator navigator;
  private LoginFormController controller;

  @BeforeAll
  static void bootstrapFx() {
    try {
      Platform.startup(() -> {});
      Platform.setImplicitExit(false);
    } catch (IllegalStateException alreadyStarted) {
      // JVM-wide singleton; another test class already started the toolkit.
    }
  }

  @BeforeEach
  void setUp() {
    authService = mock(AuthService.class);
    navigator = mock(Navigator.class);

    ServiceRegistry services = mock(ServiceRegistry.class);
    when(services.getAuthService()).thenReturn(authService);

    controller = new LoginFormController();
    ControllerWiring.wire(controller, navigator, new Session(), services);

    controller.emailField = new TextField();
    controller.passwordField = new PasswordField();
    controller.errorLabel = new Label();
  }

  @Test
  @DisplayName("Пустые поля показывают ошибку без вызова сервиса")
  void emptyFieldsShowErrorAndDoNotCallApi() {
    controller.onSubmit(new ActionEvent());

    assertTrue(controller.errorLabel.isVisible());
    assertEquals("Заполните все поля", controller.errorLabel.getText());
    verify(authService, never()).login(anyString(), anyString());
    verify(navigator, never()).goTo(anyString());
  }

  @Test
  @DisplayName("Пустой пароль показывает ошибку без вызова сервиса")
  void blankPasswordShowsErrorAndDoesNotCallApi() {
    controller.emailField.setText(VALID_EMAIL);
    controller.onSubmit(new ActionEvent());

    assertTrue(controller.errorLabel.isVisible());
    assertEquals("Заполните все поля", controller.errorLabel.getText());
    verify(authService, never()).login(anyString(), anyString());
    verify(navigator, never()).goTo(anyString());
  }

  @Test
  @DisplayName("Неверный формат почты показывает ошибку без вызова сервиса")
  void invalidEmailShowsErrorAndDoesNotCallApi() {
    controller.emailField.setText("not-an-email");
    controller.passwordField.setText(VALID_PASSWORD);
    controller.onSubmit(new ActionEvent());

    assertTrue(controller.errorLabel.isVisible());
    assertEquals("Неверный формат", controller.errorLabel.getText());
    verify(authService, never()).login(anyString(), anyString());
    verify(navigator, never()).goTo(anyString());
  }

  @Test
  @DisplayName("Успешный вход переходит к списку ресурсов")
  void successfulLoginNavigatesToResourceList() {
    User user = new User(UUID.randomUUID(), VALID_EMAIL, Role.USER, "Имя", "Фамилия", true);
    when(authService.login(VALID_EMAIL, VALID_PASSWORD)).thenReturn(user);

    controller.emailField.setText(VALID_EMAIL);
    controller.passwordField.setText(VALID_PASSWORD);
    controller.onSubmit(new ActionEvent());

    verify(authService).login(VALID_EMAIL, VALID_PASSWORD);
    verify(navigator).goTo("/fxml/ResourceList.fxml");
    assertFalse(controller.errorLabel.isVisible());
  }

  @Test
  @DisplayName("Ошибка входа показывает сообщение сервера без перехода")
  void failedLoginShowsServerErrorAndDoesNotNavigate() {
    when(authService.login(VALID_EMAIL, VALID_PASSWORD))
        .thenThrow(new ServiceException("Неверный логин или пароль"));

    controller.emailField.setText(VALID_EMAIL);
    controller.passwordField.setText(VALID_PASSWORD);
    controller.onSubmit(new ActionEvent());

    assertTrue(controller.errorLabel.isVisible());
    assertEquals("Неверный логин или пароль", controller.errorLabel.getText());
    verify(navigator, never()).goTo(anyString());
  }
}
