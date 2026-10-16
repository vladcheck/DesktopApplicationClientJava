package com.DesktopApplicationClientJava.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.DesktopApplicationClientJava.entities.Role;
import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.navigation.Navigator;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.services.ServiceRegistry;
import com.DesktopApplicationClientJava.services.UsersService;
import com.DesktopApplicationClientJava.services.data.RegisterData;
import com.DesktopApplicationClientJava.session.Session;
import com.DesktopApplicationClientJava.utils.ControllerWiring;
import com.DesktopApplicationClientJava.validation.EmailValidator;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

@Epic("Пользователи")
@Feature("Форма создания пользователя")
@Tag("role:admin")
@DisplayName("Форма создания пользователя")
class UserCreateFormControllerTest {

  private static final String VALID_EMAIL = "new.user@example.com";
  private static final String VALID_PASSWORD = "secret12345";
  private static final String VALID_FIRST = "Иван";
  private static final String VALID_LAST = "Петров";

  private UsersService usersService;
  private Navigator navigator;
  private UserCreateFormController controller;

  private TextField emailField;
  private PasswordField passwordField;
  private TextField firstNameField;
  private TextField lastNameField;
  private Label errorLabel;
  private ChoiceBox<String> rolesChoiceBox;

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
    usersService = mock(UsersService.class);
    navigator = mock(Navigator.class);

    ServiceRegistry services = mock(ServiceRegistry.class);
    when(services.getUsersService()).thenReturn(usersService);

    controller = new UserCreateFormController();
    ControllerWiring.wire(controller, navigator, new Session(), services);

    emailField = new TextField();
    passwordField = new PasswordField();
    firstNameField = new TextField();
    lastNameField = new TextField();
    errorLabel = new Label();
    rolesChoiceBox = new ChoiceBox<>();

    inject("emailField", emailField);
    inject("passwordField", passwordField);
    inject("firstNameField", firstNameField);
    inject("lastNameField", lastNameField);
    inject("errorLabel", errorLabel);
    inject("rolesChoiceBox", rolesChoiceBox);

    controller.initialize();
  }

  @Test
  @DisplayName("Инициализация подставляет роли и выбирает USER по умолчанию")
  void initializePopulatesRolesAndDefaultsToUser() {
    assertEquals(List.of("USER", "MODER", "ADMIN"), rolesChoiceBox.getItems());
    assertEquals(Role.USER.toString(), rolesChoiceBox.getValue());
  }

  @Test
  @DisplayName("Пустые поля: ошибка без создания и перехода")
  void emptyFieldsShowFillAllErrorAndDoNotCreate() {
    submit();

    assertTrue(errorLabel.isVisible());
    assertEquals("Заполните все поля", errorLabel.getText());
    verify(usersService, never()).create(any());
    verify(navigator, never()).goTo(any());
  }

  @Test
  @DisplayName("Пробельные поля: ошибка без создания и перехода")
  void blankFieldsShowFillAllErrorAndDoNotCreate() {
    emailField.setText("   ");
    passwordField.setText("   ");
    firstNameField.setText("   ");
    lastNameField.setText("   ");
    rolesChoiceBox.setValue("   ");

    submit();

    assertTrue(errorLabel.isVisible());
    assertEquals("Заполните все поля", errorLabel.getText());
    verify(usersService, never()).create(any());
    verify(navigator, never()).goTo(any());
  }

  @Test
  @DisplayName("Почта без @: ошибка валидатора без создания")
  void emailWithoutAtShowsValidatorError() {
    fillValid("nope.com", VALID_PASSWORD, VALID_FIRST, VALID_LAST, "USER");

    submit();

    assertTrue(errorLabel.isVisible());
    assertEquals(EmailValidator.validate("nope.com"), errorLabel.getText());
    verify(usersService, never()).create(any());
    verify(navigator, never()).goTo(any());
  }

  @Test
  @DisplayName("Короткая почта: ошибка валидатора без создания")
  void shortEmailShowsValidatorError() {
    fillValid("a@b", VALID_PASSWORD, VALID_FIRST, VALID_LAST, "USER");

    submit();

    assertTrue(errorLabel.isVisible());
    assertEquals(EmailValidator.validate("a@b"), errorLabel.getText());
    verify(usersService, never()).create(any());
    verify(navigator, never()).goTo(any());
  }

  @Test
  @DisplayName("Длинная почта: ошибка валидатора без создания")
  void longEmailShowsValidatorError() {
    String longEmail = "a".repeat(95) + "@x.com";
    fillValid(longEmail, VALID_PASSWORD, VALID_FIRST, VALID_LAST, "USER");

    submit();

    assertTrue(errorLabel.isVisible());
    assertEquals(EmailValidator.validate(longEmail), errorLabel.getText());
    verify(usersService, never()).create(any());
    verify(navigator, never()).goTo(any());
  }

  @Test
  @DisplayName("gap: имя из одного символа пока создаёт пользователя")
  void gapSingleCharFirstNameStillCreates() {
    User created = new User(UUID.randomUUID(), VALID_EMAIL, Role.USER, "И", VALID_LAST, true);
    when(usersService.create(any())).thenReturn(created);
    fillValid(VALID_EMAIL, VALID_PASSWORD, "И", VALID_LAST, "USER");

    submit();

    verify(usersService).create(any());
    verify(navigator).goTo("/fxml/UserList.fxml");
  }

  @Test
  @DisplayName("gap: фамилия из одного символа пока создаёт пользователя")
  void gapSingleCharLastNameStillCreates() {
    User created = new User(UUID.randomUUID(), VALID_EMAIL, Role.USER, VALID_FIRST, "П", true);
    when(usersService.create(any())).thenReturn(created);
    fillValid(VALID_EMAIL, VALID_PASSWORD, VALID_FIRST, "П", "USER");

    submit();

    verify(usersService).create(any());
    verify(navigator).goTo("/fxml/UserList.fxml");
  }

  @Test
  @DisplayName("gap: короткий пароль пока создаёт пользователя")
  void gapShortPasswordStillCreates() {
    User created =
        new User(UUID.randomUUID(), VALID_EMAIL, Role.USER, VALID_FIRST, VALID_LAST, true);
    when(usersService.create(any())).thenReturn(created);
    fillValid(VALID_EMAIL, "1234567", VALID_FIRST, VALID_LAST, "USER");

    submit();

    verify(usersService).create(any());
    verify(navigator).goTo("/fxml/UserList.fxml");
  }

  @Test
  @DisplayName("gap: длинный пароль пока создаёт пользователя")
  void gapLongPasswordStillCreates() {
    User created =
        new User(UUID.randomUUID(), VALID_EMAIL, Role.USER, VALID_FIRST, VALID_LAST, true);
    when(usersService.create(any())).thenReturn(created);
    fillValid(VALID_EMAIL, "x".repeat(65), VALID_FIRST, VALID_LAST, "USER");

    submit();

    verify(usersService).create(any());
    verify(navigator).goTo("/fxml/UserList.fxml");
  }

  @Test
  @DisplayName("Роль null: ошибка заполнения без создания")
  void nullRoleShowsFillAllError() {
    fillValid(VALID_EMAIL, VALID_PASSWORD, VALID_FIRST, VALID_LAST, null);

    submit();

    assertTrue(errorLabel.isVisible());
    assertEquals("Заполните все поля", errorLabel.getText());
    verify(usersService, never()).create(any());
    verify(navigator, never()).goTo(any());
  }

  @Test
  @DisplayName("Пустая роль: ошибка заполнения без создания")
  void emptyRoleShowsFillAllError() {
    fillValid(VALID_EMAIL, VALID_PASSWORD, VALID_FIRST, VALID_LAST, "");

    submit();

    assertTrue(errorLabel.isVisible());
    assertEquals("Заполните все поля", errorLabel.getText());
    verify(usersService, never()).create(any());
    verify(navigator, never()).goTo(any());
  }

  @Test
  @DisplayName("Неизвестная роль: ошибка без создания")
  void unknownRoleShowsUndefinedError() {
    fillValid(VALID_EMAIL, VALID_PASSWORD, VALID_FIRST, VALID_LAST, "SUPERUSER");

    submit();

    assertTrue(errorLabel.isVisible());
    assertEquals("Роль не определена", errorLabel.getText());
    verify(usersService, never()).create(any());
    verify(navigator, never()).goTo(any());
  }

  @Test
  @DisplayName("Роль в нижнем регистре: ошибка без создания")
  void lowercaseRoleShowsUndefinedError() {
    fillValid(VALID_EMAIL, VALID_PASSWORD, VALID_FIRST, VALID_LAST, "user");

    submit();

    assertTrue(errorLabel.isVisible());
    assertEquals("Роль не определена", errorLabel.getText());
    verify(usersService, never()).create(any());
    verify(navigator, never()).goTo(any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"USER", "MODER", "ADMIN"})
  @DisplayName("Валидные роли передаются в сервис создания")
  void validRolesArePassedToCreate(String roleName) {
    User created =
        new User(
            UUID.randomUUID(), VALID_EMAIL, Role.valueOf(roleName), VALID_FIRST, VALID_LAST, true);
    when(usersService.create(any())).thenReturn(created);
    fillValid(VALID_EMAIL, VALID_PASSWORD, VALID_FIRST, VALID_LAST, roleName);

    submit();

    ArgumentCaptor<RegisterData> captor = ArgumentCaptor.forClass(RegisterData.class);
    verify(usersService).create(captor.capture());
    RegisterData data = captor.getValue();
    assertEquals(VALID_EMAIL, data.getEmail());
    assertEquals(VALID_PASSWORD, data.getPassword());
    assertEquals(Role.valueOf(roleName), data.getRole());
    assertEquals(VALID_FIRST, data.getFirstName());
    assertEquals(VALID_LAST, data.getLastName());
    verify(navigator).goTo("/fxml/UserList.fxml");
  }

  @Test
  @DisplayName("Успех скрывает ошибку и ведёт в список пользователей")
  void successHidesErrorAndNavigatesToUserList() {
    User created =
        new User(UUID.randomUUID(), VALID_EMAIL, Role.USER, VALID_FIRST, VALID_LAST, true);
    when(usersService.create(any())).thenReturn(created);
    errorLabel.setText("старая ошибка");
    errorLabel.setVisible(true);
    errorLabel.setManaged(true);
    fillValid(VALID_EMAIL, VALID_PASSWORD, VALID_FIRST, VALID_LAST, "USER");

    submit();

    verify(usersService).create(any());
    verify(navigator).goTo("/fxml/UserList.fxml");
    assertFalse(errorLabel.isVisible());
    assertEquals("", errorLabel.getText());
  }

  @Test
  @DisplayName("Пустой ответ сервиса: без навигации")
  void nullUserDoesNotNavigate() {
    when(usersService.create(any())).thenReturn(null);
    fillValid(VALID_EMAIL, VALID_PASSWORD, VALID_FIRST, VALID_LAST, "USER");

    submit();

    verify(usersService).create(any());
    verify(navigator, never()).goTo(any());
  }

  @Test
  @DisplayName("Дубликат почты: ошибка без перехода")
  void duplicateEmailShowsErrorAndDoesNotNavigate() {
    when(usersService.create(any())).thenThrow(new ServiceException("Пользователь уже существует"));
    fillValid(VALID_EMAIL, VALID_PASSWORD, VALID_FIRST, VALID_LAST, "USER");

    submit();

    assertTrue(errorLabel.isVisible());
    assertEquals("Пользователь уже существует", errorLabel.getText());
    verify(navigator, never()).goTo(any());
  }

  @Test
  @DisplayName("Сетевая ошибка: сообщение без перехода")
  void networkErrorShowsErrorAndDoesNotNavigate() {
    when(usersService.create(any())).thenThrow(new ServiceException("Сеть недоступна"));
    fillValid(VALID_EMAIL, VALID_PASSWORD, VALID_FIRST, VALID_LAST, "USER");

    submit();

    assertTrue(errorLabel.isVisible());
    assertEquals("Сеть недоступна", errorLabel.getText());
    verify(navigator, never()).goTo(any());
  }

  @Test
  @DisplayName("Отмена ведёт в список без создания")
  void cancelNavigatesToListWithoutCreating() {
    fillValid(VALID_EMAIL, VALID_PASSWORD, VALID_FIRST, VALID_LAST, "USER");

    controller.onCancel(new ActionEvent());

    verify(navigator).goTo("/fxml/UserList.fxml");
    verify(usersService, never()).create(any());
  }

  private void fillValid(
      String email, String password, String firstName, String lastName, String role) {
    emailField.setText(email);
    passwordField.setText(password);
    firstNameField.setText(firstName);
    lastNameField.setText(lastName);
    rolesChoiceBox.setValue(role);
  }

  private void submit() {
    try {
      Method method =
          UserCreateFormController.class.getDeclaredMethod("onSubmit", ActionEvent.class);
      method.setAccessible(true);
      method.invoke(controller, new ActionEvent());
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Не удалось вызвать onSubmit", e);
    }
  }

  private void inject(String fieldName, Object value) {
    try {
      Field field = UserCreateFormController.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(controller, value);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Не удалось инжектить " + fieldName, e);
    }
  }
}
