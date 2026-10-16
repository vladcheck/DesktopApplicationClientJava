package com.DesktopApplicationClientJava.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.DesktopApplicationClientJava.controllers.UserDetailsController;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.util.WaitForAsyncUtils;

/**
 * Компонентные тесты страницы пользователя: удаление через настоящую модалку подтверждения (роль
 * АДМИН).
 */
@Epic("Пользователи")
@Feature("Страница пользователя")
@DisplayName("Детали пользователя: удаление через модалку")
class UserDetailsControllerUiTest extends FxTestBase {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final String EMAIL = "ivan@example.com";

  private AuthService authService;
  private UsersService usersService;
  private ServiceRegistry services;
  private Navigator navigator;
  private Session session;
  private TestableUserDetailsController controller;
  private Stage stage;

  static class TestableUserDetailsController extends UserDetailsController {
    boolean stubConfirm = true;
    boolean confirmDeleteResult = false;
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
    usersService = mock(UsersService.class);
    services = mock(ServiceRegistry.class);
    when(services.getAuthService()).thenReturn(authService);
    when(services.getUsersService()).thenReturn(usersService);
    navigator = mock(Navigator.class);
    session = new Session();
    session.setCurrentUser(user());
    session.setAccessToken("access");
    session.setRefreshToken("refresh");
    this.stage = stage;
    when(authService.isAdmin()).thenReturn(true);
    when(authService.getCurrentUser()).thenReturn(null);
    when(usersService.getById(USER_ID)).thenReturn(user());
    loadPage();
    controller.setUserId(USER_ID);
  }

  private void loadPage() {
    try {
      controller = new TestableUserDetailsController();
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserDetails.fxml"));
      loader.setControllerFactory(
          clazz -> {
            ControllerWiring.wire(controller, navigator, session, services);
            return controller;
          });
      Parent root = loader.load();
      stage.setScene(new Scene(root));
      stage.show();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  @Tag("role:admin")
  @DisplayName("Админ видит кнопки редактирования и удаления")
  void adminSeesEditAndDeleteButtons() {
    awaitLoaded();

    assertTrue(robot.lookup("#editButton").queryAs(Button.class).isVisible());
    assertTrue(robot.lookup("#deleteButton").queryAs(Button.class).isVisible());
  }

  @Test
  @Tag("role:user")
  @DisplayName("Пользователь не видит кнопок редактирования и удаления")
  void plainUserSeesNeitherEditNorDeleteButtons() {
    robot.interact(
        () -> {
          when(authService.isAdmin()).thenReturn(false);
          loadPage();
        });

    Button edit = robot.lookup("#editButton").queryAs(Button.class);
    Button delete = robot.lookup("#deleteButton").queryAs(Button.class);
    assertFalse(edit.isVisible());
    assertFalse(edit.isManaged());
    assertFalse(delete.isVisible());
    assertFalse(delete.isManaged());
  }

  @Test
  @Tag("role:moder")
  @DisplayName("Модер не видит кнопок редактирования и удаления")
  void moderSeesNeitherEditNorDeleteButtons() {
    robot.interact(
        () -> {
          when(authService.isAdmin()).thenReturn(false);
          loadPage();
        });

    Button edit = robot.lookup("#editButton").queryAs(Button.class);
    Button delete = robot.lookup("#deleteButton").queryAs(Button.class);
    assertFalse(edit.isVisible());
    assertFalse(edit.isManaged());
    assertFalse(delete.isVisible());
    assertFalse(delete.isManaged());
  }

  @Test
  @Tag("role:admin")
  @DisplayName("Клик админа открывает модалку с почтой и предупреждением")
  void adminDeleteClickOpensModalWithEmailAndWarning() {
    awaitLoaded();
    robot.interact(() -> controller.stubConfirm = false);

    Thread clicker = clickDeleteNoWait();
    DialogPane dialog = waitForDialog(robot);

    assertTrue(dialog.getScene().getWindow().isShowing(), "модалка удаления не открылась");
    assertEquals("Удаление пользователя", titleOf(dialog));
    String content = dialog.getContentText();
    assertTrue(content.contains(EMAIL), "в модалке нет почты пользователя");
    assertTrue(content.contains("Действие необратимо"), "в модалке нет предупреждения");

    pressDialogButtonByText(robot, dialog, "Отмена");
    waitForClicker(clicker);

    assertModalClosed(robot);
    assertEquals(1, controller.confirmCalls);
    verify(usersService, never()).delete(USER_ID);
    verify(navigator, never()).goTo(anyString());
  }

  @Test
  @Tag("role:admin")
  @DisplayName("Подтверждение удаляет пользователя и ведёт в список")
  void confirmingModalDeletesAndNavigatesToList() {
    awaitLoaded();
    robot.interact(() -> controller.stubConfirm = false);

    Thread clicker = clickDeleteNoWait();
    DialogPane dialog = waitForDialog(robot);
    pressDialogButtonByText(robot, dialog, "Удалить");
    waitForClicker(clicker);

    assertModalClosed(robot);
    awaitNavigation();
    verify(usersService).delete(USER_ID);
    verify(navigator).goTo("/fxml/UserList.fxml");
  }

  @Test
  @Tag("role:admin")
  @DisplayName("Удаление без прав ведёт на вход с очищенной сессией")
  void deleteForbiddenRedirectsToLogin() {
    awaitLoaded();
    doThrow(new ServiceException("Доступ запрещён", 403)).when(usersService).delete(USER_ID);
    robot.interact(() -> controller.stubConfirm = false);

    Thread clicker = clickDeleteNoWait();
    DialogPane dialog = waitForDialog(robot);
    pressDialogButtonByText(robot, dialog, "Удалить");
    waitForClicker(clicker);

    assertModalClosed(robot);
    awaitLoginRedirect();
    verify(navigator).goToLoginOnce();
    assertNull(session.getCurrentUser());
    assertFalse(robot.lookup("#errorLabel").queryAs(Label.class).isVisible());
  }

  @Test
  @Tag("role:admin")
  @DisplayName("Удаление отсутствующего показывает ошибку без перехода")
  void deleteMissingShowsErrorWithoutNavigation() {
    awaitLoaded();
    doThrow(new ServiceException("Пользователь не найден", 404)).when(usersService).delete(USER_ID);
    robot.interact(() -> controller.stubConfirm = false);

    Thread clicker = clickDeleteNoWait();
    DialogPane dialog = waitForDialog(robot);
    pressDialogButtonByText(robot, dialog, "Удалить");
    waitForClicker(clicker);

    assertModalClosed(robot);
    awaitError("Пользователь не найден");
    verify(navigator, never()).goTo(anyString());
  }

  private User user() {
    return new User(USER_ID, EMAIL, Role.ADMIN, "Иван", "Петров", true);
  }

  private Label fullName() {
    return robot.lookup("#fullNameLabel").queryAs(Label.class);
  }

  private Label error() {
    return robot.lookup("#errorLabel").queryAs(Label.class);
  }

  private void awaitLoaded() {
    try {
      WaitForAsyncUtils.waitFor(
          30, TimeUnit.SECONDS, () -> !"...".equals(fullName().getText()) || error().isVisible());
    } catch (TimeoutException e) {
      throw new AssertionError("Не дождались загрузки пользователя", e);
    }
  }

  private void awaitNavigation() {
    try {
      WaitForAsyncUtils.waitFor(
          30, TimeUnit.SECONDS, () -> !mockingDetails(navigator).getInvocations().isEmpty());
    } catch (TimeoutException e) {
      throw new AssertionError("Не дождались навигации", e);
    }
  }

  private void awaitError(String expected) {
    try {
      WaitForAsyncUtils.waitFor(
          30, TimeUnit.SECONDS, () -> expected.equals(error().getText()) && error().isVisible());
    } catch (TimeoutException e) {
      throw new AssertionError("Не дождались ошибки: " + expected, e);
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

  private static String titleOf(DialogPane dialog) {
    return ((Stage) dialog.getScene().getWindow()).getTitle();
  }

  /**
   * Нажимает кнопку удаления, не дожидаясь закрытия модального окна: настоящий {@code Alert}
   * блокирует поток JavaFX через {@code showAndWait}, поэтому клик уходит в отдельный поток, а тест
   * продолжает работать с диалогом.
   */
  private Thread clickDeleteNoWait() {
    Button delete = robot.lookup("#deleteButton").queryAs(Button.class);
    Thread clicker = new Thread(() -> robot.interact(delete::fire), "user-details-delete-click");
    clicker.setDaemon(true);
    clicker.start();
    return clicker;
  }

  private static DialogPane waitForDialog(FxRobot robot) {
    try {
      WaitForAsyncUtils.waitFor(
          30, TimeUnit.SECONDS, () -> !robot.lookup(".dialog-pane").tryQuery().isEmpty());
    } catch (TimeoutException e) {
      throw new AssertionError("Не дождались модального окна подтверждения удаления", e);
    }
    return robot.lookup(".dialog-pane").queryAs(DialogPane.class);
  }

  private static void pressDialogButtonByText(FxRobot robot, DialogPane dialog, String text) {
    ButtonType type =
        dialog.getButtonTypes().stream()
            .filter(buttonType -> text.equals(buttonType.getText()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Нет кнопки «" + text + "» в модалке"));
    robot.interact(() -> ((Button) dialog.lookupButton(type)).fire());
  }

  private static void waitForClicker(Thread clicker) {
    try {
      WaitForAsyncUtils.waitFor(30, TimeUnit.SECONDS, () -> !clicker.isAlive());
    } catch (TimeoutException e) {
      throw new AssertionError("Клик по удалению не завершился после закрытия диалога", e);
    }
  }

  private static void assertModalClosed(FxRobot robot) {
    assertTrue(robot.lookup(".dialog-pane").tryQuery().isEmpty(), "модалка удаления не закрылась");
  }
}
