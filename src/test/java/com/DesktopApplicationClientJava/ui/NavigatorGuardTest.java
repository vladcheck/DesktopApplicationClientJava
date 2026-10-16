package com.DesktopApplicationClientJava.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.DesktopApplicationClientJava.entities.Role;
import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.navigation.Navigator;
import com.DesktopApplicationClientJava.services.AuthService;
import com.DesktopApplicationClientJava.services.ResourceService;
import com.DesktopApplicationClientJava.services.ServiceRegistry;
import com.DesktopApplicationClientJava.services.UsersService;
import com.DesktopApplicationClientJava.session.Session;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxToolkit;
import org.testfx.util.WaitForAsyncUtils;

@Epic("Безопасность")
@Feature("Гард навигации")
@DisplayName("Гард Navigator.goTo по ролям")
class NavigatorGuardTest extends FxTestBase {

  private static final User ADMIN =
      new User(UUID.randomUUID(), "admin@example.com", Role.ADMIN, "Имя", "Фамилия", true);
  private static final User MODERATOR =
      new User(UUID.randomUUID(), "moder@example.com", Role.MODER, "Имя", "Фамилия", true);
  private static final User PLAIN_USER =
      new User(UUID.randomUUID(), "user@example.com", Role.USER, "Имя", "Фамилия", true);

  private AuthService authService;
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
    authService = mock(AuthService.class);
    UsersService usersService = mock(UsersService.class);
    ResourceService resourceService = mock(ResourceService.class);
    services = mock(ServiceRegistry.class);
    when(services.getAuthService()).thenReturn(authService);
    when(services.getUsersService()).thenReturn(usersService);
    when(services.getResourceService()).thenReturn(resourceService);
    when(usersService.search(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.anyLong()))
        .thenReturn(List.of());
    when(resourceService.search(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of());
    session = new Session();
    this.stage = stage;
    navigator = new Navigator(stage, session, services);
    stubGuest();
    robot.interact(
        () -> {
          stage.setScene(new Scene(new StackPane()));
          stage.show();
        });
  }

  private void stubRole(User user, boolean admin, boolean moderator) {
    when(authService.getCurrentUser()).thenReturn(user);
    when(authService.isAdmin()).thenReturn(admin);
    when(authService.isModerator()).thenReturn(moderator);
    session.setCurrentUser(user);
  }

  private void stubGuest() {
    stubRole(null, false, false);
  }

  private void goTo(String fxml) {
    robot.interact(() -> navigator.goTo(fxml));
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

  private boolean present(String query) {
    try {
      robot.lookup(query).query();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private boolean onResourceList() {
    return present("#resourceTable");
  }

  private boolean onUserList() {
    return present("#usersTable");
  }

  private boolean onLoginForm() {
    return present("#submitButton")
        && present("#passwordField")
        && !present("#firstNameField")
        && !present("#resourceTable")
        && !present("#usersTable");
  }

  private boolean onResourceForm() {
    return present("#titleField") && present("#formTitleLabel");
  }

  @Test
  @Tag("role:user")
  @DisplayName("Пользователь прямым переходом не открывает список пользователей")
  void userCannotOpenUserListDirectly() {
    robot.interact(() -> stubRole(PLAIN_USER, false, false));
    goTo("/fxml/UserList.fxml");
    waitForNode("#resourceTable");

    assertTrue(onResourceList(), "ожидался редирект на список ресурсов");
    assertFalse(onUserList(), "список пользователей должен быть отклонён");
  }

  @Test
  @Tag("role:moder")
  @DisplayName("Модер прямым переходом не открывает список пользователей")
  void moderatorCannotOpenUserListDirectly() {
    robot.interact(() -> stubRole(MODERATOR, false, true));
    goTo("/fxml/UserList.fxml");
    waitForNode("#resourceTable");

    assertTrue(onResourceList(), "ожидался редирект на список ресурсов");
    assertFalse(onUserList(), "список пользователей должен быть отклонён");
  }

  @Test
  @Tag("role:guest")
  @DisplayName("Гость прямым переходом на список пользователей уходит на вход")
  void guestUserListRedirectsToLogin() {
    robot.interact(this::stubGuest);
    goTo("/fxml/UserList.fxml");
    waitForNode("#submitButton");

    assertTrue(onLoginForm(), "гость должен оказаться на форме входа");
    assertFalse(onUserList(), "список пользователей должен быть отклонён");
  }

  @Test
  @Tag("role:user")
  @DisplayName("Пользователь прямым переходом не открывает детали пользователя")
  void userCannotOpenUserDetailsDirectly() {
    robot.interact(() -> stubRole(PLAIN_USER, false, false));
    goTo("/fxml/UserDetails.fxml");
    waitForNode("#resourceTable");

    assertTrue(onResourceList(), "ожидался редирект на список ресурсов");
    assertFalse(present("#roleLabel"), "детали пользователя должны быть отклонены");
  }

  @Test
  @Tag("role:user")
  @DisplayName("Пользователь прямым переходом не открывает форму создания пользователя")
  void userCannotOpenUserCreateFormDirectly() {
    robot.interact(() -> stubRole(PLAIN_USER, false, false));
    goTo("/fxml/UserCreateForm.fxml");
    waitForNode("#resourceTable");

    assertTrue(onResourceList(), "ожидался редирект на список ресурсов");
    assertFalse(present("#rolesChoiceBox"), "форма создания должна быть отклонена");
  }

  @Test
  @Tag("role:user")
  @DisplayName("Пользователь прямым переходом не открывает форму обновления пользователя")
  void userCannotOpenUserUpdateFormDirectly() {
    robot.interact(() -> stubRole(PLAIN_USER, false, false));
    goTo("/fxml/UserUpdateForm.fxml");
    waitForNode("#resourceTable");

    assertTrue(onResourceList(), "ожидался редирект на список ресурсов");
    assertFalse(present("#roleChoiceBox"), "форма обновления должна быть отклонена");
  }

  @Test
  @Tag("role:user")
  @DisplayName("Пользователь прямым переходом не открывает форму ресурса")
  void userCannotOpenResourceFormDirectly() {
    robot.interact(() -> stubRole(PLAIN_USER, false, false));
    goTo("/fxml/ResourceForm.fxml");
    waitForNode("#resourceTable");

    assertTrue(onResourceList(), "ожидался редирект на список ресурсов");
    assertFalse(onResourceForm(), "форма ресурса должна быть отклонена");
  }

  @Test
  @Tag("role:guest")
  @DisplayName("Гость прямым переходом на форму ресурса уходит на вход")
  void guestResourceFormRedirectsToLogin() {
    robot.interact(this::stubGuest);
    goTo("/fxml/ResourceForm.fxml");
    waitForNode("#submitButton");

    assertTrue(onLoginForm(), "гость должен оказаться на форме входа");
    assertFalse(onResourceForm(), "форма ресурса должна быть отклонена");
  }

  @Test
  @Tag("role:moder")
  @DisplayName("Модер открывает форму ресурса напрямую")
  void moderatorCanOpenResourceFormDirectly() {
    robot.interact(() -> stubRole(MODERATOR, false, true));
    goTo("/fxml/ResourceForm.fxml");
    waitForNode("#titleField");

    assertTrue(onResourceForm(), "модер должен открыть форму ресурса");
  }

  @Test
  @Tag("role:admin")
  @DisplayName("Админ открывает список пользователей напрямую")
  void adminCanOpenUserListDirectly() {
    robot.interact(() -> stubRole(ADMIN, true, false));
    goTo("/fxml/UserList.fxml");
    waitForNode("#usersTable");

    assertTrue(onUserList(), "админ должен открыть список пользователей");
  }

  @Test
  @Tag("role:guest")
  @DisplayName("Без AuthService прямой переход fail-closed уходит на вход")
  void nullAuthServiceRedirectsToLogin() {
    robot.interact(() -> when(services.getAuthService()).thenReturn(null));
    goTo("/fxml/UserList.fxml");
    waitForNode("#submitButton");

    assertTrue(onLoginForm(), "без AuthService должен быть редирект на вход");
  }

  @Test
  @Tag("role:guest")
  @DisplayName("Без сессии прямой переход fail-closed уходит на вход")
  void nullSessionRedirectsToLogin() {
    robot.interact(
        () -> {
          stubRole(PLAIN_USER, false, false);
          navigator = new Navigator(stage, null, services);
        });
    goTo("/fxml/UserList.fxml");
    waitForNode("#submitButton");

    assertTrue(onLoginForm(), "без сессии должен быть редирект на вход");
  }
}
