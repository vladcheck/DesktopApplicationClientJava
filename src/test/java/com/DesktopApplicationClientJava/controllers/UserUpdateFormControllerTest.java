package com.DesktopApplicationClientJava.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.DesktopApplicationClientJava.Api.ApiClient;
import com.DesktopApplicationClientJava.Api.Dto.request.Admin.UpdateUserRequest;
import com.DesktopApplicationClientJava.Api.Dto.response.UserDto;
import com.DesktopApplicationClientJava.entities.Role;
import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.navigation.Navigator;
import com.DesktopApplicationClientJava.services.AuthService;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.services.ServiceRegistry;
import com.DesktopApplicationClientJava.services.UsersService;
import com.DesktopApplicationClientJava.services.data.UpdateUserData;
import com.DesktopApplicationClientJava.services.impl.UsersServiceImpl;
import com.DesktopApplicationClientJava.session.Session;
import com.DesktopApplicationClientJava.utils.ControllerWiring;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.testfx.util.WaitForAsyncUtils;

@Epic("Пользователи")
@Feature("Редактирование пользователя")
@DisplayName("Форма редактирования пользователя")
class UserUpdateFormControllerTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final String EMAIL = "ivan@example.com";
  private static final String FIRST_NAME = "Иван";
  private static final String LAST_NAME = "Петров";

  private UsersService usersService;
  private AuthService authService;
  private Navigator navigator;
  private UserUpdateFormController controller;

  private TextField emailField;
  private TextField firstNameField;
  private TextField lastNameField;
  private ChoiceBox<Role> roleChoiceBox;
  private CheckBox enabledCheckBox;
  private Label subtitleLabel;
  private Label errorLabel;
  private Button saveButton;
  private Button cancelButton;

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
    authService = mock(AuthService.class);
    navigator = mock(Navigator.class);

    ServiceRegistry services = mock(ServiceRegistry.class);
    when(services.getUsersService()).thenReturn(usersService);
    when(services.getAuthService()).thenReturn(authService);
    when(authService.getCurrentUser()).thenReturn(null);

    controller = new UserUpdateFormController();
    ControllerWiring.wire(controller, navigator, new Session(), services);

    emailField = new TextField();
    firstNameField = new TextField();
    lastNameField = new TextField();
    roleChoiceBox = new ChoiceBox<>();
    enabledCheckBox = new CheckBox();
    subtitleLabel = new Label();
    errorLabel = new Label();
    saveButton = new Button();
    cancelButton = new Button();

    inject(controller, "emailField", emailField);
    inject(controller, "firstNameField", firstNameField);
    inject(controller, "lastNameField", lastNameField);
    inject(controller, "roleChoiceBox", roleChoiceBox);
    inject(controller, "enabledCheckBox", enabledCheckBox);
    inject(controller, "subtitleLabel", subtitleLabel);
    inject(controller, "errorLabel", errorLabel);
    inject(controller, "saveButton", saveButton);
    inject(controller, "cancelButton", cancelButton);

    invokeNoArg("initialize");
  }

  // ===== Загрузка =====

  @Test
  @DisplayName("Загрузка заполняет все поля формы")
  void loadPopulatesAllFields() {
    when(usersService.getById(USER_ID)).thenReturn(existingUser());
    controller.setUserId(USER_ID);
    awaitLoaded();

    assertEquals(EMAIL, emailField.getText());
    assertEquals(FIRST_NAME, firstNameField.getText());
    assertEquals(LAST_NAME, lastNameField.getText());
    assertEquals(Role.MODER, roleChoiceBox.getValue());
    assertTrue(enabledCheckBox.isSelected());
    assertEquals(EMAIL, subtitleLabel.getText());
    assertFalse(errorLabel.isVisible());
    assertFalse(emailField.isDisabled());
    assertFalse(saveButton.isDisabled());
  }

  @Test
  @DisplayName("Пустой флаг активности превращается в выключенный чекбокс")
  void nullEnabledBecomesUnchecked() {
    when(usersService.getById(USER_ID))
        .thenReturn(new User(USER_ID, EMAIL, Role.USER, FIRST_NAME, LAST_NAME, null));
    controller.setUserId(USER_ID);
    awaitLoaded();

    assertFalse(enabledCheckBox.isSelected());
  }

  @Test
  @DisplayName("Пустые поля пользователя превращаются в пустые строки")
  void nullFieldsBecomeEmptyStrings() {
    when(usersService.getById(USER_ID)).thenReturn(new User(USER_ID, null, null, null, null, null));
    controller.setUserId(USER_ID);
    awaitLoaded();

    assertEquals("", emailField.getText());
    assertEquals("", firstNameField.getText());
    assertEquals("", lastNameField.getText());
    assertNull(roleChoiceBox.getValue());
    assertFalse(enabledCheckBox.isSelected());
    assertEquals("", subtitleLabel.getText());
  }

  @Test
  @DisplayName("Пустой идентификатор ничего не загружает")
  void nullUserIdDoesNothing() {
    controller.setUserId(null);

    verifyNoInteractions(usersService);
    verifyNoInteractions(navigator);
    assertEquals("", emailField.getText());
    assertFalse(errorLabel.isVisible());
  }

  @Test
  @DisplayName("Загрузка без сервисов не падает")
  void loadWithoutServicesDoesNothing() {
    UserUpdateFormController bare = new UserUpdateFormController();
    ControllerWiring.wire(bare, navigator, new Session(), null);
    bare.setUserId(USER_ID);

    verifyNoInteractions(navigator);
  }

  @Test
  @DisplayName("Загрузка без users-сервиса не падает")
  void loadWithoutUsersServiceDoesNothing() {
    ServiceRegistry emptyServices = mock(ServiceRegistry.class);
    Navigator bareNavigator = mock(Navigator.class);
    UserUpdateFormController bare = new UserUpdateFormController();
    ControllerWiring.wire(bare, bareNavigator, new Session(), emptyServices);

    bare.setUserId(USER_ID);

    verify(emptyServices).getUsersService();
    verifyNoInteractions(bareNavigator);
  }

  @Test
  @DisplayName("Инициализация заполняет список ролей")
  void initializeFillsRoles() {
    assertEquals(List.of(Role.values()), new ArrayList<>(roleChoiceBox.getItems()));
  }

  // ===== Валидация =====

  @Test
  @DisplayName("Пустая почта: ошибка без сохранения")
  void blankEmailShowsErrorAndDoesNotSave() {
    loadForValidation();
    emailField.setText("   ");
    invokeHandler("onSave");

    assertTrue(errorLabel.isVisible());
    assertEquals("Укажите почту", errorLabel.getText());
    verify(usersService, never()).update(any(), any());
    verify(navigator, never()).goTo(anyString());
  }

  @Test
  @DisplayName("Почта без @: ошибка без сохранения")
  void emailWithoutAtShowsErrorAndDoesNotSave() {
    loadForValidation();
    emailField.setText("ivan.example.com");
    invokeHandler("onSave");

    assertTrue(errorLabel.isVisible());
    assertEquals("Почта указана некорректно", errorLabel.getText());
    verify(usersService, never()).update(any(), any());
    verify(navigator, never()).goTo(anyString());
  }

  @Test
  @DisplayName("Пустое имя: ошибка без сохранения")
  void blankFirstNameShowsErrorAndDoesNotSave() {
    loadForValidation();
    firstNameField.setText("  ");
    invokeHandler("onSave");

    assertTrue(errorLabel.isVisible());
    assertEquals("Укажите имя", errorLabel.getText());
    verify(usersService, never()).update(any(), any());
    verify(navigator, never()).goTo(anyString());
  }

  @Test
  @DisplayName("Пустая фамилия: ошибка без сохранения")
  void blankLastNameShowsErrorAndDoesNotSave() {
    loadForValidation();
    lastNameField.setText("");
    invokeHandler("onSave");

    assertTrue(errorLabel.isVisible());
    assertEquals("Укажите фамилию", errorLabel.getText());
    verify(usersService, never()).update(any(), any());
    verify(navigator, never()).goTo(anyString());
  }

  @Test
  @DisplayName("Роль не выбрана: ошибка без сохранения")
  void missingRoleShowsErrorAndDoesNotSave() {
    loadForValidation();
    roleChoiceBox.getSelectionModel().clearSelection();
    invokeHandler("onSave");

    assertTrue(errorLabel.isVisible());
    assertEquals("Выберите роль", errorLabel.getText());
    verify(usersService, never()).update(any(), any());
    verify(navigator, never()).goTo(anyString());
  }

  @Test
  @DisplayName("Приоритет: первая ошибка — пустая почта")
  void firstViolationWins() {
    loadForValidation();
    emailField.setText("");
    firstNameField.setText("");
    lastNameField.setText("");
    roleChoiceBox.getSelectionModel().clearSelection();
    invokeHandler("onSave");

    assertTrue(errorLabel.isVisible());
    assertEquals("Укажите почту", errorLabel.getText());
    verify(usersService, never()).update(any(), any());
  }

  @Test
  @DisplayName("Сохранение до загрузки ничего не делает")
  void saveBeforeLoadDoesNothing() {
    emailField.setText(EMAIL);
    firstNameField.setText(FIRST_NAME);
    lastNameField.setText(LAST_NAME);
    roleChoiceBox.getSelectionModel().select(Role.USER);
    invokeHandler("onSave");

    verifyNoInteractions(usersService);
    verifyNoInteractions(navigator);
    assertFalse(errorLabel.isVisible());
  }

  @Test
  @DisplayName("Короткая почта с @ проходит валидацию (gap: нет проверки длины)")
  void shortEmailPassesValidation() {
    loadForValidation();
    when(usersService.update(eq(USER_ID), any())).thenReturn(existingUser());
    emailField.setText("a@b");
    invokeHandler("onSave");

    awaitNavigation();
    verify(usersService).update(eq(USER_ID), any(UpdateUserData.class));
  }

  @Test
  @DisplayName("Имя из одного символа проходит валидацию (gap: нет проверки длины)")
  void singleCharFirstNamePassesValidation() {
    loadForValidation();
    when(usersService.update(eq(USER_ID), any())).thenReturn(existingUser());
    firstNameField.setText("И");
    invokeHandler("onSave");

    awaitNavigation();
    ArgumentCaptor<UpdateUserData> captor = ArgumentCaptor.forClass(UpdateUserData.class);
    verify(usersService).update(eq(USER_ID), captor.capture());
    assertEquals("И", captor.getValue().getFirstName());
  }

  // ===== Успех =====

  @Test
  @DisplayName("Успешное сохранение вызывает update и ведёт в список пользователей")
  void successfulSaveUpdatesAndNavigatesToList() {
    when(usersService.getById(USER_ID)).thenReturn(existingUser());
    when(usersService.update(eq(USER_ID), any())).thenReturn(existingUser());
    controller.setUserId(USER_ID);
    awaitLoaded();

    firstNameField.setText("Пётр");
    invokeHandler("onSave");

    awaitNavigation();
    ArgumentCaptor<UpdateUserData> captor = ArgumentCaptor.forClass(UpdateUserData.class);
    verify(usersService).update(eq(USER_ID), captor.capture());
    UpdateUserData data = captor.getValue();
    assertEquals(EMAIL, data.getEmail());
    assertEquals(Role.MODER, data.getRole());
    assertEquals("Пётр", data.getFirstName());
    assertEquals(LAST_NAME, data.getLastName());
    assertEquals(true, data.getEnabled());
  }

  @Test
  @DisplayName("Пробелы по краям полей обрезаются перед сохранением")
  void surroundingWhitespaceIsTrimmed() {
    when(usersService.getById(USER_ID)).thenReturn(existingUser());
    when(usersService.update(eq(USER_ID), any())).thenReturn(existingUser());
    controller.setUserId(USER_ID);
    awaitLoaded();

    emailField.setText("  ivan@example.com  ");
    firstNameField.setText("  Иван  ");
    lastNameField.setText("  Петров  ");
    invokeHandler("onSave");

    awaitNavigation();
    ArgumentCaptor<UpdateUserData> captor = ArgumentCaptor.forClass(UpdateUserData.class);
    verify(usersService).update(eq(USER_ID), captor.capture());
    UpdateUserData data = captor.getValue();
    assertEquals("ivan@example.com", data.getEmail());
    assertEquals("Иван", data.getFirstName());
    assertEquals("Петров", data.getLastName());
  }

  @Test
  @DisplayName("UUID для обновления берётся из загруженного пользователя")
  void updateUuidComesFromLoadedUser() {
    UUID otherId = UUID.randomUUID();
    when(usersService.getById(USER_ID))
        .thenReturn(new User(otherId, EMAIL, Role.USER, FIRST_NAME, LAST_NAME, false));
    when(usersService.update(eq(otherId), any())).thenReturn(existingUser());
    controller.setUserId(USER_ID);
    awaitLoaded();

    invokeHandler("onSave");

    awaitNavigation();
    verify(usersService).update(eq(otherId), any(UpdateUserData.class));
  }

  @Test
  @DisplayName("Переключение чекбокса активности отправляется в сервис")
  void enabledToggleIsSentToService() {
    when(usersService.getById(USER_ID)).thenReturn(existingUser());
    when(usersService.update(eq(USER_ID), any())).thenReturn(existingUser());
    controller.setUserId(USER_ID);
    awaitLoaded();

    enabledCheckBox.setSelected(false);
    invokeHandler("onSave");

    awaitNavigation();
    ArgumentCaptor<UpdateUserData> captor = ArgumentCaptor.forClass(UpdateUserData.class);
    verify(usersService).update(eq(USER_ID), captor.capture());
    assertEquals(false, captor.getValue().getEnabled());
  }

  @Test
  @DisplayName("Форма блокируется на время сохранения и разблокируется после успеха")
  void formIsDisabledWhileSaving() {
    when(usersService.getById(USER_ID)).thenReturn(existingUser());
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    when(usersService.update(eq(USER_ID), any()))
        .thenAnswer(
            invocation -> {
              entered.countDown();
              assertTrue(release.await(10, TimeUnit.SECONDS), "Сохранение не дождалось релиза");
              return existingUser();
            });
    controller.setUserId(USER_ID);
    awaitLoaded();

    invokeHandler("onSave");

    await(() -> entered.getCount() == 0, "Сервис обновления не был вызван");
    assertTrue(emailField.isDisabled());
    assertTrue(saveButton.isDisabled());

    release.countDown();
    awaitNavigation();
    assertFalse(emailField.isDisabled());
    assertFalse(saveButton.isDisabled());
  }

  // ===== Частичное обновление и маппинг =====

  @Test
  @DisplayName("Неизменённые поля отправляются как есть")
  void unchangedFieldsAreSentAsIs() {
    when(usersService.getById(USER_ID)).thenReturn(existingUser());
    when(usersService.update(eq(USER_ID), any())).thenReturn(existingUser());
    controller.setUserId(USER_ID);
    awaitLoaded();

    lastNameField.setText("Сидоров");
    invokeHandler("onSave");

    awaitNavigation();
    ArgumentCaptor<UpdateUserData> captor = ArgumentCaptor.forClass(UpdateUserData.class);
    verify(usersService).update(eq(USER_ID), captor.capture());
    UpdateUserData data = captor.getValue();
    assertEquals(EMAIL, data.getEmail());
    assertEquals(Role.MODER, data.getRole());
    assertEquals(FIRST_NAME, data.getFirstName());
    assertEquals("Сидоров", data.getLastName());
    assertEquals(true, data.getEnabled());
  }

  @Test
  @DisplayName("Сохранение без изменений всё равно вызывает update")
  void saveWithoutChangesStillCallsUpdate() {
    when(usersService.getById(USER_ID)).thenReturn(existingUser());
    when(usersService.update(eq(USER_ID), any())).thenReturn(existingUser());
    controller.setUserId(USER_ID);
    awaitLoaded();

    invokeHandler("onSave");

    awaitNavigation();
    ArgumentCaptor<UpdateUserData> captor = ArgumentCaptor.forClass(UpdateUserData.class);
    verify(usersService).update(eq(USER_ID), captor.capture());
    UpdateUserData data = captor.getValue();
    assertEquals(EMAIL, data.getEmail());
    assertEquals(Role.MODER, data.getRole());
    assertEquals(FIRST_NAME, data.getFirstName());
    assertEquals(LAST_NAME, data.getLastName());
    assertEquals(true, data.getEnabled());
  }

  @Test
  @DisplayName("UpdateUserData отображается в UpdateUserRequest для ApiClient")
  void updateUserDataMapsToUpdateUserRequest() throws Exception {
    ApiClient apiClient = mock(ApiClient.class);
    UsersServiceImpl service = new UsersServiceImpl(apiClient);
    LocalDateTime now = LocalDateTime.now();
    UserDto dto =
        new UserDto(USER_ID, "new@example.com", Role.ADMIN, "Иван", "Петров", false, now, now);
    when(apiClient.updateUser(eq(USER_ID), any())).thenReturn(dto);

    UpdateUserData data =
        new UpdateUserData("new@example.com", Role.ADMIN, "Иван", "Петров", false);
    User result = service.update(USER_ID, data);

    ArgumentCaptor<UpdateUserRequest> captor = ArgumentCaptor.forClass(UpdateUserRequest.class);
    verify(apiClient).updateUser(eq(USER_ID), captor.capture());
    UpdateUserRequest request = captor.getValue();
    assertEquals("new@example.com", request.email());
    assertEquals(Role.ADMIN, request.role());
    assertEquals("Иван", request.firstname());
    assertEquals("Петров", request.lastname());
    assertEquals(false, request.enabled());
    assertEquals(USER_ID, result.getUuid());
  }

  // ===== Ошибки загрузки =====

  @Test
  @DisplayName("Загрузка: 404 показывает текст ошибки, форма остаётся заблокированной")
  void loadNotFoundShowsErrorAndStaysDisabled() {
    when(usersService.getById(USER_ID))
        .thenThrow(new ServiceException("Пользователь не найден", 404));
    controller.setUserId(USER_ID);

    awaitError("Пользователь не найден");
    verify(navigator, never()).goTo(anyString());
    assertTrue(saveButton.isDisabled());
    assertTrue(emailField.isDisabled());
  }

  @Test
  @DisplayName("Загрузка: 403 показывает текст ошибки без перехода")
  void loadForbiddenShowsErrorWithoutNavigation() {
    when(usersService.getById(USER_ID)).thenThrow(new ServiceException("Доступ запрещён", 403));
    controller.setUserId(USER_ID);

    awaitError("Доступ запрещён");
    verify(navigator, never()).goTo(anyString());
    assertTrue(saveButton.isDisabled());
  }

  @Test
  @DisplayName("Загрузка: ошибка сети показывает текст ошибки без перехода")
  void loadNetworkErrorShowsErrorWithoutNavigation() {
    when(usersService.getById(USER_ID))
        .thenThrow(new ServiceException("Ошибка соединения с сервером"));
    controller.setUserId(USER_ID);

    awaitError("Ошибка соединения с сервером");
    verify(navigator, never()).goTo(anyString());
    assertTrue(saveButton.isDisabled());
  }

  @Test
  @DisplayName("Пустой ответ при загрузке: пользователь не найден")
  void nullLoadResultShowsNotFound() {
    when(usersService.getById(USER_ID)).thenReturn(null);
    controller.setUserId(USER_ID);

    awaitError("Пользователь не найден");
    verify(navigator, never()).goTo(anyString());
    assertTrue(saveButton.isDisabled());
  }

  // ===== Ошибки сохранения =====

  @Test
  @DisplayName("Сохранение: 404 показывает текст ошибки, форма разблокируется")
  void saveNotFoundShowsErrorAndReenables() {
    stubLoadedAndFailingUpdate(new ServiceException("Пользователь не найден", 404));
    invokeHandler("onSave");

    awaitError("Пользователь не найден");
    verify(navigator, never()).goTo(anyString());
    assertFalse(saveButton.isDisabled());
    assertFalse(emailField.isDisabled());
  }

  @Test
  @DisplayName("Сохранение: 403 показывает текст ошибки без перехода")
  void saveForbiddenShowsErrorWithoutNavigation() {
    stubLoadedAndFailingUpdate(new ServiceException("Доступ запрещён", 403));
    invokeHandler("onSave");

    awaitError("Доступ запрещён");
    verify(navigator, never()).goTo(anyString());
    assertFalse(saveButton.isDisabled());
  }

  @Test
  @DisplayName("Сохранение: 409 показывает текст ошибки без перехода")
  void saveConflictShowsErrorWithoutNavigation() {
    stubLoadedAndFailingUpdate(new ServiceException("Почта уже занята", 409));
    invokeHandler("onSave");

    awaitError("Почта уже занята");
    verify(navigator, never()).goTo(anyString());
    assertFalse(saveButton.isDisabled());
  }

  @Test
  @DisplayName("Сохранение: ошибка сети показывает текст ошибки, форма разблокируется")
  void saveNetworkErrorShowsErrorAndReenables() {
    stubLoadedAndFailingUpdate(new ServiceException("Ошибка соединения с сервером"));
    invokeHandler("onSave");

    awaitError("Ошибка соединения с сервером");
    verify(navigator, never()).goTo(anyString());
    assertFalse(saveButton.isDisabled());
    assertFalse(emailField.isDisabled());
  }

  // ===== Самодаунгрейд и самоблокировка (#95) =====

  @Test
  @DisplayName("Своя запись: смена роли с ADMIN на другую блокируется")
  void selfRoleDowngradeIsBlocked() {
    User me = new User(USER_ID, EMAIL, Role.ADMIN, FIRST_NAME, LAST_NAME, true);
    when(authService.getCurrentUser()).thenReturn(me);
    when(usersService.getById(USER_ID)).thenReturn(me);
    controller.setUserId(USER_ID);
    awaitLoaded();

    roleChoiceBox.getSelectionModel().select(Role.USER);
    invokeHandler("onSave");

    assertTrue(errorLabel.isVisible());
    assertEquals("Нельзя снять себе роль администратора", errorLabel.getText());
    verify(usersService, never()).update(any(), any());
    verify(navigator, never()).goTo(anyString());
  }

  @Test
  @DisplayName("Своя запись: снятие активности себе блокируется")
  void selfDisableIsBlocked() {
    User me = new User(USER_ID, EMAIL, Role.ADMIN, FIRST_NAME, LAST_NAME, true);
    when(authService.getCurrentUser()).thenReturn(me);
    when(usersService.getById(USER_ID)).thenReturn(me);
    controller.setUserId(USER_ID);
    awaitLoaded();

    enabledCheckBox.setSelected(false);
    invokeHandler("onSave");

    assertTrue(errorLabel.isVisible());
    assertEquals("Нельзя отключить собственную учётную запись", errorLabel.getText());
    verify(usersService, never()).update(any(), any());
    verify(navigator, never()).goTo(anyString());
  }

  @Test
  @DisplayName("Чужой пользователь: смена роли и активности не блокируется")
  void otherUserRoleAndEnabledChangeIsNotBlocked() {
    User me =
        new User(UUID.randomUUID(), "admin@example.com", Role.ADMIN, "Админ", "Главный", true);
    when(authService.getCurrentUser()).thenReturn(me);
    when(usersService.getById(USER_ID)).thenReturn(existingUser());
    when(usersService.update(eq(USER_ID), any())).thenReturn(existingUser());
    controller.setUserId(USER_ID);
    awaitLoaded();

    roleChoiceBox.getSelectionModel().select(Role.USER);
    enabledCheckBox.setSelected(false);
    invokeHandler("onSave");

    awaitNavigation();
    verify(usersService).update(eq(USER_ID), any(UpdateUserData.class));
  }

  @Test
  @DisplayName("Своя запись: смена имени и почты не блокируется")
  void selfNameAndEmailChangeIsNotBlocked() {
    User me = new User(USER_ID, EMAIL, Role.ADMIN, FIRST_NAME, LAST_NAME, true);
    when(authService.getCurrentUser()).thenReturn(me);
    when(usersService.getById(USER_ID)).thenReturn(me);
    when(usersService.update(eq(USER_ID), any())).thenReturn(me);
    controller.setUserId(USER_ID);
    awaitLoaded();

    firstNameField.setText("Пётр");
    emailField.setText("petr@example.com");
    invokeHandler("onSave");

    awaitNavigation();
    ArgumentCaptor<UpdateUserData> captor = ArgumentCaptor.forClass(UpdateUserData.class);
    verify(usersService).update(eq(USER_ID), captor.capture());
    assertEquals("Пётр", captor.getValue().getFirstName());
    assertEquals("petr@example.com", captor.getValue().getEmail());
  }

  // ===== Отмена =====

  @Test
  @DisplayName("Отмена ведёт в список пользователей без сохранения")
  void cancelNavigatesToListWithoutSaving() {
    loadForValidation();
    lastNameField.setText("Сидоров");
    invokeHandler("onCancel");

    verify(navigator).goTo("/fxml/UserList.fxml");
    verify(usersService, never()).update(any(), any());
  }

  // ===== Хелперы =====

  private void loadForValidation() {
    when(usersService.getById(USER_ID)).thenReturn(existingUser());
    controller.setUserId(USER_ID);
    awaitLoaded();
  }

  private void stubLoadedAndFailingUpdate(ServiceException failure) {
    when(usersService.getById(USER_ID)).thenReturn(existingUser());
    when(usersService.update(eq(USER_ID), any())).thenThrow(failure);
    controller.setUserId(USER_ID);
    awaitLoaded();
  }

  private void awaitLoaded() {
    await(() -> !saveButton.isDisabled(), "Не дождались загрузки пользователя");
  }

  private void awaitError(String expected) {
    await(errorLabel::isVisible, "Не дождались ошибки «" + expected + "»");
    assertEquals(expected, errorLabel.getText());
  }

  private void awaitNavigation() {
    await(
        () -> {
          try {
            verify(navigator).goTo("/fxml/UserList.fxml");
            return true;
          } catch (AssertionError e) {
            return false;
          }
        },
        "Не дождались перехода в список пользователей");
  }

  private static void await(Callable<Boolean> condition, String message) {
    try {
      WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, condition);
    } catch (TimeoutException e) {
      throw new AssertionError(message, e);
    }
  }

  private void invokeNoArg(String name) {
    try {
      Method method = UserUpdateFormController.class.getDeclaredMethod(name);
      method.setAccessible(true);
      method.invoke(controller);
    } catch (InvocationTargetException e) {
      rethrow(name, e);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError("Не вызван метод " + name, e);
    }
  }

  private void invokeHandler(String name) {
    try {
      Method method = UserUpdateFormController.class.getDeclaredMethod(name, ActionEvent.class);
      method.setAccessible(true);
      method.invoke(controller, new ActionEvent());
    } catch (InvocationTargetException e) {
      rethrow(name, e);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError("Не вызван метод " + name, e);
    }
  }

  private static void rethrow(String name, InvocationTargetException e) {
    Throwable cause = e.getCause();
    if (cause instanceof RuntimeException re) {
      throw re;
    }
    if (cause instanceof Error err) {
      throw err;
    }
    throw new AssertionError("Не вызван метод " + name, cause);
  }

  private static void inject(UserUpdateFormController target, String name, Object value) {
    try {
      Field field = UserUpdateFormController.class.getDeclaredField(name);
      field.setAccessible(true);
      field.set(target, value);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError("Нет поля " + name + " в UserUpdateFormController", e);
    }
  }

  private static User existingUser() {
    return new User(USER_ID, EMAIL, Role.MODER, FIRST_NAME, LAST_NAME, true);
  }
}
