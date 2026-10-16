package com.DesktopApplicationClientJava.controllers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.DesktopApplicationClientJava.entities.Role;
import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.navigation.Navigator;
import com.DesktopApplicationClientJava.services.AuthService;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.services.ServiceRegistry;
import com.DesktopApplicationClientJava.services.UsersService;
import com.DesktopApplicationClientJava.session.Session;
import com.DesktopApplicationClientJava.utils.ControllerWiring;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.testfx.util.WaitForAsyncUtils;

/**
 * Headless-тесты страницы пользователя: реальный FXML, диалоги подтверждаются стабом (без модалки
 * {@code Alert}), асинхронная загрузка ожидается только через {@code WaitForAsyncUtils}.
 */
@Epic("Пользователи")
@Feature("Страница пользователя")
@DisplayName("Детали пользователя")
class UserDetailsControllerTest {

  private static final UUID USER_ID = UUID.randomUUID();

  private AuthService authService;
  private UsersService usersService;
  private ServiceRegistry services;
  private Navigator navigator;
  private Session session;
  private TestableUserDetailsController controller;
  private Parent root;

  static class TestableUserDetailsController extends UserDetailsController {
    boolean stubConfirm = true;
    boolean confirmDeleteResult = true;
    int confirmCalls = 0;

    @Override
    protected boolean confirmDelete(User user) {
      confirmCalls++;
      if (stubConfirm) {
        return confirmDeleteResult;
      }
      return super.confirmDelete(user);
    }
  }

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
    usersService = mock(UsersService.class);
    services = mock(ServiceRegistry.class);
    when(services.getAuthService()).thenReturn(authService);
    when(services.getUsersService()).thenReturn(usersService);
    navigator = mock(Navigator.class);
    session = new Session();
    session.setCurrentUser(user());
    session.setAccessToken("access");
    session.setRefreshToken("refresh");
    when(authService.isAdmin()).thenReturn(true);
    when(authService.getCurrentUser()).thenReturn(null);
  }

  @Test
  @Tag("role:admin")
  @DisplayName("Загрузка по id показывает все поля пользователя")
  void loadByIdShowsAllFields() {
    loadUser(user());

    assertEquals("Иван Петров", label("#fullNameLabel").getText());
    assertEquals("ivan@example.com", label("#emailLabel").getText());
    assertEquals("ADMIN", label("#roleLabel").getText());
    assertEquals("Да", label("#enabledLabel").getText());
    assertEquals(USER_ID.toString(), label("#uuidLabel").getText());
    assertFalse(label("#errorLabel").isVisible());
    assertTrue(label("#fullNameLabel").isVisible());
  }

  @Test
  @Tag("role:admin")
  @DisplayName("Во время загрузки виден индикатор «Загрузка…»")
  void loadingIndicatorShownDuringRequest() throws Exception {
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    when(usersService.getById(USER_ID))
        .thenAnswer(
            invocation -> {
              entered.countDown();
              assertTrue(release.await(30, TimeUnit.SECONDS), "Не дождались разрешения");
              return user();
            });
    loadPage();
    fxSetUserId(USER_ID);

    assertTrue(entered.await(30, TimeUnit.SECONDS), "Сервис не вызван");
    assertEquals("Загрузка…", label("#statusLabel").getText());
    release.countDown();
    awaitLoaded();

    assertEquals("Иван Петров", label("#fullNameLabel").getText());
  }

  @Test
  @Tag("role:admin")
  @DisplayName("setUserId(null) не обращается к сервису")
  void nullIdDoesNotCallService() {
    loadPage();
    fxSetUserId(null);

    verifyNoInteractions(usersService);
    assertFalse(label("#errorLabel").isVisible());
  }

  @Test
  @Tag("role:admin")
  @DisplayName("Вызов setUserId до wiring не бросает NPE")
  void setUserIdBeforeWiringDoesNotThrow() {
    TestableUserDetailsController raw = new TestableUserDetailsController();

    assertDoesNotThrow(() -> raw.setUserId(USER_ID));
    verifyNoInteractions(usersService, navigator);
  }

  @Test
  @Tag("role:admin")
  @DisplayName("Пустое имя показывается как «Без имени»")
  void blankNameShowsPlaceholder() {
    loadUser(new User(USER_ID, "ivan@example.com", Role.USER, "", " ", true));

    assertEquals("Без имени", label("#fullNameLabel").getText());
  }

  @Test
  @Tag("role:admin")
  @DisplayName("Null-поля отображаются пустыми, enabled null — «Нет»")
  void nullFieldsShownEmpty() {
    loadUser(new User(null, null, null, null, null, null));

    assertEquals("Без имени", label("#fullNameLabel").getText());
    assertEquals("", label("#emailLabel").getText());
    assertEquals("", label("#roleLabel").getText());
    assertEquals("Нет", label("#enabledLabel").getText());
    assertEquals("", label("#uuidLabel").getText());
  }

  @Test
  @Tag("role:admin")
  @DisplayName("enabled false показывается как «Нет»")
  void disabledFalseShowsNo() {
    loadUser(new User(USER_ID, "ivan@example.com", Role.USER, "Иван", "Петров", false));

    assertEquals("Нет", label("#enabledLabel").getText());
  }

  @Test
  @Tag("role:admin")
  @DisplayName("Null-ответ сервиса: «Пользователь не найден»")
  void nullUserShowsNotFound() {
    when(usersService.getById(USER_ID)).thenReturn(null);
    loadPage();
    fxSetUserId(USER_ID);
    awaitLoaded();

    assertTrue(label("#errorLabel").isVisible());
    assertEquals("Пользователь не найден", label("#errorLabel").getText());
    verifyNoInteractions(navigator);
  }

  @Test
  @Tag("role:admin")
  @DisplayName("ServiceException 403 ведёт на вход с очищенной сессией")
  void serviceExceptionForbiddenRedirectsToLogin() {
    when(usersService.getById(USER_ID)).thenThrow(new ServiceException("Нет доступа", 403));
    loadPage();
    fxSetUserId(USER_ID);
    awaitLoginRedirect();

    verify(navigator).goToLoginOnce();
    assertNull(session.getCurrentUser());
    assertFalse(label("#errorLabel").isVisible());
  }

  @Test
  @Tag("role:admin")
  @DisplayName("Runtime-исключение показывает общую ошибку загрузки")
  void runtimeExceptionShowsGenericMessage() {
    when(usersService.getById(USER_ID)).thenThrow(new RuntimeException("boom"));
    loadPage();
    fxSetUserId(USER_ID);
    awaitLoaded();

    assertTrue(label("#errorLabel").isVisible());
    assertEquals("Не удалось загрузить пользователя", label("#errorLabel").getText());
    verifyNoInteractions(navigator);
  }

  @Test
  @Tag("role:admin")
  @DisplayName("Кнопка «Назад» ведёт в список пользователей")
  void backNavigatesToUserList() {
    loadUser(user());
    fire("#backButton");

    verify(navigator).goTo("/fxml/UserList.fxml");
  }

  @Test
  @Tag("role:admin")
  @DisplayName("Админ видит кнопки редактирования и удаления")
  void adminSeesEditAndDeleteButtons() {
    loadPage();

    assertTrue(button("#editButton").isVisible());
    assertTrue(button("#editButton").isManaged());
    assertTrue(button("#deleteButton").isVisible());
    assertTrue(button("#deleteButton").isManaged());
  }

  @Test
  @Tag("role:user")
  @DisplayName("Пользователь не видит кнопок редактирования и удаления")
  void plainUserSeesNeitherEditNorDeleteButtons() {
    when(authService.isAdmin()).thenReturn(false);
    loadPage();

    assertFalse(button("#editButton").isVisible());
    assertFalse(button("#editButton").isManaged());
    assertFalse(button("#deleteButton").isVisible());
    assertFalse(button("#deleteButton").isManaged());
  }

  @Test
  @Tag("role:moder")
  @DisplayName("Модер не видит кнопок редактирования и удаления")
  void moderSeesNeitherEditNorDeleteButtons() {
    when(authService.isAdmin()).thenReturn(false);
    loadPage();

    assertFalse(button("#editButton").isVisible());
    assertFalse(button("#editButton").isManaged());
    assertFalse(button("#deleteButton").isVisible());
    assertFalse(button("#deleteButton").isManaged());
  }

  @Test
  @Tag("role:admin")
  @DisplayName("Null AuthService прячет кнопки без NPE")
  void nullAuthServiceHidesButtonsWithoutNpe() {
    when(services.getAuthService()).thenReturn(null);
    when(usersService.getById(USER_ID)).thenReturn(user());
    loadPage();
    fxSetUserId(USER_ID);
    awaitLoaded();

    assertFalse(button("#editButton").isVisible());
    assertFalse(button("#deleteButton").isVisible());
    assertEquals("Иван Петров", label("#fullNameLabel").getText());
  }

  @Test
  @Tag("role:admin")
  @DisplayName("Своя запись: delete заблокирован даже при подтверждении")
  void selfDeleteIsBlockedEvenWhenConfirmed() {
    User me = new User(USER_ID, "me@example.com", Role.ADMIN, "Я", "Админ", true);
    when(authService.getCurrentUser()).thenReturn(me);
    when(usersService.getById(USER_ID)).thenReturn(me);
    loadPage();
    fxSetUserId(USER_ID);
    awaitLoaded();

    assertTrue(button("#deleteButton").isDisable());
    controller.confirmDeleteResult = true;
    // Кнопка disabled игнорирует fire(), поэтому эмулируем обход блокировки —
    // контроллер обязан отклонить удаление себя guard-веткой.
    runOnFx(
        () -> {
          button("#deleteButton").setDisable(false);
          button("#deleteButton").fire();
        });

    assertEquals(0, controller.confirmCalls);
    assertTrue(label("#errorLabel").isVisible());
    assertEquals("Нельзя удалить собственную учётную запись", label("#errorLabel").getText());
    verify(usersService, never()).delete(any());
    verify(navigator, never()).goTo(anyString());
  }

  @Test
  @Tag("role:admin")
  @DisplayName("Редактирование ведёт на форму с userId")
  void editNavigatesToFormWithUserId() {
    loadUser(user());
    fire("#editButton");

    ArgumentCaptor<Consumer<Object>> captor = ArgumentCaptor.forClass(Consumer.class);
    verify(navigator).goTo(eq("/fxml/UserUpdateForm.fxml"), captor.capture());
    UserUpdateFormController form = mock(UserUpdateFormController.class);
    captor.getValue().accept(form);
    verify(form).setUserId(USER_ID);
  }

  @Test
  @Tag("role:admin")
  @DisplayName("Редактирование без current ничего не делает")
  void editWithoutCurrentDoesNothing() {
    when(usersService.getById(USER_ID)).thenReturn(null);
    loadPage();
    fxSetUserId(USER_ID);
    awaitLoaded();
    fire("#editButton");

    verifyNoInteractions(navigator);
  }

  @Test
  @Tag("role:admin")
  @DisplayName("Отменённое удаление не вызывает сервис")
  void cancelledDeleteDoesNotCallService() {
    loadUser(user());
    controller.confirmDeleteResult = false;
    fire("#deleteButton");

    assertEquals(1, controller.confirmCalls);
    verify(usersService, never()).delete(any());
    verify(navigator, never()).goTo(anyString());
  }

  @Test
  @Tag("role:admin")
  @DisplayName("Подтверждённое удаление вызывает сервис и ведёт в список")
  void confirmedDeleteCallsServiceAndNavigatesToList() {
    loadUser(user());
    controller.confirmDeleteResult = true;
    fire("#deleteButton");
    awaitUnbusy();

    verify(usersService).delete(USER_ID);
    verify(navigator).goTo("/fxml/UserList.fxml");
    assertFalse(label("#errorLabel").isVisible());
  }

  @Test
  @Tag("role:admin")
  @DisplayName("Ошибка 403 ведёт на вход с очищенной сессией")
  void deleteForbiddenRedirectsToLogin() {
    loadUser(user());
    controller.confirmDeleteResult = true;
    doThrow(new ServiceException("Доступ запрещён", 403)).when(usersService).delete(USER_ID);
    fire("#deleteButton");
    awaitLoginRedirect();

    verify(navigator).goToLoginOnce();
    assertNull(session.getCurrentUser());
    assertFalse(label("#errorLabel").isVisible());
  }

  @Test
  @Tag("role:admin")
  @DisplayName("Ошибка 404 показывает текст без перехода")
  void deleteMissingShowsMessageWithoutNavigation() {
    loadUser(user());
    controller.confirmDeleteResult = true;
    doThrow(new ServiceException("Пользователь не найден", 404)).when(usersService).delete(USER_ID);
    fire("#deleteButton");
    awaitUnbusy();

    assertTrue(label("#errorLabel").isVisible());
    assertEquals("Пользователь не найден", label("#errorLabel").getText());
    verify(navigator, never()).goTo(anyString());
  }

  private void loadUser(User user) {
    when(usersService.getById(USER_ID)).thenReturn(user);
    loadPage();
    fxSetUserId(USER_ID);
    awaitLoaded();
  }

  private void loadPage() {
    runOnFx(
        () -> {
          try {
            controller = new TestableUserDetailsController();
            ControllerWiring.wire(controller, navigator, session, services);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserDetails.fxml"));
            loader.setControllerFactory(type -> controller);
            root = loader.load();
            new Scene(root);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  private void fxSetUserId(UUID id) {
    runOnFx(() -> controller.setUserId(id));
  }

  private void fire(String query) {
    runOnFx(() -> button(query).fire());
  }

  private Label label(String query) {
    return (Label) root.lookup(query);
  }

  private Button button(String query) {
    return (Button) root.lookup(query);
  }

  private void awaitLoaded() {
    try {
      WaitForAsyncUtils.waitFor(
          30,
          TimeUnit.SECONDS,
          () ->
              "".equals(label("#statusLabel").getText())
                  && (!"...".equals(label("#fullNameLabel").getText())
                      || label("#errorLabel").isVisible()));
    } catch (TimeoutException e) {
      throw new AssertionError("Не дождались загрузки пользователя", e);
    }
  }

  private void awaitUnbusy() {
    try {
      WaitForAsyncUtils.waitFor(
          30,
          TimeUnit.SECONDS,
          () -> !button("#deleteButton").isDisable() || label("#errorLabel").isVisible());
    } catch (TimeoutException e) {
      throw new AssertionError("Не дождались завершения удаления", e);
    }
  }

  private void awaitLoginRedirect() {
    try {
      WaitForAsyncUtils.waitFor(
          30,
          TimeUnit.SECONDS,
          () -> {
            try {
              verify(navigator).goToLoginOnce();
              return true;
            } catch (AssertionError e) {
              return false;
            }
          });
    } catch (TimeoutException e) {
      throw new AssertionError("Не дождались перехода на вход", e);
    }
  }

  private static void runOnFx(Runnable action) {
    try {
      WaitForAsyncUtils.asyncFx(action).get(30, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError("FX-поток прерван", e);
    } catch (ExecutionException | TimeoutException e) {
      throw new AssertionError("Не дождались FX-потока", e);
    }
  }

  private static User user() {
    return new User(USER_ID, "ivan@example.com", Role.ADMIN, "Иван", "Петров", true);
  }
}
