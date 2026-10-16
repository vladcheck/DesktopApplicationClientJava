package com.DesktopApplicationClientJava.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.DesktopApplicationClientJava.controllers.ResourceListController;
import com.DesktopApplicationClientJava.controllers.UsersListController;
import com.DesktopApplicationClientJava.entities.Resource;
import com.DesktopApplicationClientJava.entities.Role;
import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.navigation.Navigator;
import com.DesktopApplicationClientJava.services.AuthService;
import com.DesktopApplicationClientJava.services.ResourceService;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.services.ServiceRegistry;
import com.DesktopApplicationClientJava.services.UsersService;
import com.DesktopApplicationClientJava.session.Session;
import com.DesktopApplicationClientJava.utils.ControllerWiring;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.testfx.api.FxToolkit;
import org.testfx.util.WaitForAsyncUtils;

@Epic("Администрирование")
@Feature("Навигация администратора")
@DisplayName("Кнопки навигации администратора")
class AdminNavigationButtonsTest extends FxTestBase {

  private static final User ADMIN =
      new User(UUID.randomUUID(), "admin@example.com", Role.ADMIN, "Имя", "Фамилия", true);
  private static final User MODERATOR =
      new User(UUID.randomUUID(), "moder@example.com", Role.MODER, "Имя", "Фамилия", true);
  private static final User PLAIN_USER =
      new User(UUID.randomUUID(), "user@example.com", Role.USER, "Имя", "Фамилия", true);

  private AuthService authService;
  private ResourceService resourceService;
  private UsersService usersService;
  private ServiceRegistry services;
  private Navigator navigator;
  private Session session;
  private Stage stage;

  private ResourceListController resourceController;
  private UsersListController usersController;

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
    resourceService = mock(ResourceService.class);
    usersService = mock(UsersService.class);
    services = mock(ServiceRegistry.class);
    when(services.getAuthService()).thenReturn(authService);
    when(services.getResourceService()).thenReturn(resourceService);
    when(services.getUsersService()).thenReturn(usersService);
    navigator = mock(Navigator.class);
    session = new Session();
    this.stage = stage;
    stubAdmin();
    loadResourcePage();
  }

  private void stubAdmin() {
    stubRole(ADMIN, true, false);
  }

  private void stubRole(User user, boolean admin, boolean moderator) {
    when(authService.getCurrentUser()).thenReturn(user);
    when(authService.isAdmin()).thenReturn(admin);
    when(authService.isModerator()).thenReturn(moderator);
  }

  private void loadResourcePage() {
    try {
      resourceController = new ResourceListController();
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ResourceList.fxml"));
      loader.setControllerFactory(
          clazz -> {
            ControllerWiring.wire(resourceController, navigator, session, services);
            return resourceController;
          });
      Parent root = loader.load();
      stage.setScene(new Scene(root));
      stage.show();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void loadUsersPage() {
    try {
      usersController = new UsersListController();
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserList.fxml"));
      loader.setControllerFactory(
          clazz -> {
            ControllerWiring.wire(usersController, navigator, session, services);
            return usersController;
          });
      Parent root = loader.load();
      stage.setScene(new Scene(root));
      stage.show();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  // ===== ResourceList: usersButton виден только админу =====

  @Test
  @Tag("role:admin")
  @DisplayName("Админ видит кнопку «Пользователи» на списке ресурсов")
  void adminSeesUsersButton() {
    Button users = robot.lookup("#usersButton").queryAs(Button.class);

    assertTrue(users.isVisible());
    assertTrue(users.isManaged());
  }

  @Test
  @Tag("role:moder")
  @DisplayName("Модер не видит кнопку «Пользователи» на списке ресурсов")
  void moderatorDoesNotSeeUsersButton() {
    robot.interact(
        () -> {
          stubRole(MODERATOR, false, true);
          loadResourcePage();
        });

    Button users = robot.lookup("#usersButton").queryAs(Button.class);
    assertFalse(users.isVisible());
    assertFalse(users.isManaged());
  }

  @Test
  @Tag("role:user")
  @DisplayName("Пользователь не видит кнопку «Пользователи» на списке ресурсов")
  void plainUserDoesNotSeeUsersButton() {
    robot.interact(
        () -> {
          stubRole(PLAIN_USER, false, false);
          loadResourcePage();
        });

    Button users = robot.lookup("#usersButton").queryAs(Button.class);
    assertFalse(users.isVisible());
    assertFalse(users.isManaged());
  }

  // ===== UserList: resourcesButton виден только админу (тикет #74) =====

  @Test
  @Tag("role:admin")
  @DisplayName("Админ видит кнопку «Документы» на списке пользователей")
  void adminSeesResourcesButton() {
    robot.interact(this::loadUsersPage);

    Button resources = robot.lookup("#resourcesButton").queryAs(Button.class);
    assertTrue(resources.isVisible());
    assertTrue(resources.isManaged());
  }

  @Test
  @Tag("role:moder")
  @DisplayName("Модер не видит кнопку «Документы» на списке пользователей")
  void moderatorDoesNotSeeResourcesButton() {
    robot.interact(
        () -> {
          stubRole(MODERATOR, false, true);
          loadUsersPage();
        });

    Button resources = robot.lookup("#resourcesButton").queryAs(Button.class);
    assertFalse(resources.isVisible());
    assertFalse(resources.isManaged());
  }

  @Test
  @Tag("role:user")
  @DisplayName("Пользователь не видит кнопку «Документы» на списке пользователей")
  void plainUserDoesNotSeeResourcesButton() {
    robot.interact(
        () -> {
          stubRole(PLAIN_USER, false, false);
          loadUsersPage();
        });

    Button resources = robot.lookup("#resourcesButton").queryAs(Button.class);
    assertFalse(resources.isVisible());
    assertFalse(resources.isManaged());
  }

  // ===== Навигация =====

  @Test
  @Story("Переход между списками")
  @Tag("role:admin")
  @DisplayName("Кнопка «Пользователи» ведёт на список пользователей без выхода")
  void usersButtonNavigatesToUserList() {
    fireButton("#usersButton");

    verify(navigator).goTo("/fxml/UserList.fxml");
    verify(authService, never()).logout();
  }

  @Test
  @Story("Переход между списками")
  @Tag("role:admin")
  @DisplayName("Кнопка «Документы» ведёт на список ресурсов без выхода")
  void resourcesButtonNavigatesToResourceList() {
    robot.interact(this::loadUsersPage);
    fireButton("#resourcesButton");

    verify(navigator).goTo("/fxml/ResourceList.fxml");
    verify(authService, never()).logout();
  }

  @Test
  @Story("Переход между списками")
  @Tag("role:admin")
  @DisplayName("Круговая навигация: ресурсы → пользователи → ресурсы")
  void circularNavigationGoesBothWays() {
    fireButton("#usersButton");
    robot.interact(this::loadUsersPage);
    fireButton("#resourcesButton");

    InOrder inOrder = inOrder(navigator);
    inOrder.verify(navigator).goTo("/fxml/UserList.fxml");
    inOrder.verify(navigator).goTo("/fxml/ResourceList.fxml");
    verify(authService, never()).logout();
  }

  @Test
  @Tag("role:moder")
  @DisplayName("Модер не может перейти на список пользователей")
  void nonAdminCannotNavigateToUserList() {
    robot.interact(
        () -> {
          stubRole(MODERATOR, false, true);
          loadResourcePage();
        });
    fireButton("#usersButton");

    verify(navigator, never()).goTo("/fxml/UserList.fxml");
  }

  @Test
  @Tag("role:moder")
  @DisplayName("Модер не может перейти на список ресурсов")
  void nonAdminCannotNavigateToResourceList() {
    robot.interact(
        () -> {
          stubRole(MODERATOR, false, true);
          loadUsersPage();
        });
    fireButton("#resourcesButton");

    verify(navigator, never()).goTo("/fxml/ResourceList.fxml");
  }

  // ===== Null-safety =====

  @Test
  @Tag("role:user")
  @DisplayName("Список ресурсов без AuthService: кнопка скрыта, NPE нет")
  void resourceListWithoutAuthServiceHidesButton() {
    robot.interact(
        () -> {
          when(services.getAuthService()).thenReturn(null);
          loadResourcePage();
        });

    Button users = robot.lookup("#usersButton").queryAs(Button.class);
    assertFalse(users.isVisible());
    assertFalse(users.isManaged());
  }

  @Test
  @Tag("role:user")
  @DisplayName("Список ресурсов без текущего пользователя: кнопка скрыта, NPE нет")
  void resourceListWithoutCurrentUserHidesButton() {
    robot.interact(
        () -> {
          stubRole(null, false, false);
          loadResourcePage();
        });

    Button users = robot.lookup("#usersButton").queryAs(Button.class);
    assertFalse(users.isVisible());
    assertFalse(users.isManaged());
    assertEquals("", textOf("#userInfoLabel"));
  }

  @Test
  @Tag("role:user")
  @DisplayName("Список ресурсов с пустой ролью: кнопка скрыта, NPE нет")
  void resourceListWithNullRoleHidesButton() {
    User noRole = new User(UUID.randomUUID(), "norole@example.com", null, "Имя", "Фамилия", true);
    robot.interact(
        () -> {
          stubRole(noRole, false, false);
          loadResourcePage();
        });

    Button users = robot.lookup("#usersButton").queryAs(Button.class);
    assertFalse(users.isVisible());
    assertFalse(users.isManaged());
  }

  @Test
  @Tag("role:user")
  @DisplayName("Список пользователей без AuthService: кнопка скрыта, NPE нет")
  void usersListWithoutAuthServiceHidesButton() {
    robot.interact(
        () -> {
          when(services.getAuthService()).thenReturn(null);
          loadUsersPage();
        });

    Button resources = robot.lookup("#resourcesButton").queryAs(Button.class);
    assertFalse(resources.isVisible());
    assertFalse(resources.isManaged());
  }

  @Test
  @Tag("role:user")
  @DisplayName("Список пользователей без текущего пользователя: кнопка скрыта, NPE нет")
  void usersListWithoutCurrentUserHidesButton() {
    robot.interact(
        () -> {
          stubRole(null, false, false);
          loadUsersPage();
        });

    Button resources = robot.lookup("#resourcesButton").queryAs(Button.class);
    assertFalse(resources.isVisible());
    assertFalse(resources.isManaged());
    assertEquals("", textOf("#userInfoLabel"));
  }

  @Test
  @Tag("role:user")
  @DisplayName("Список пользователей с пустой ролью: кнопка скрыта, NPE нет")
  void usersListWithNullRoleHidesButton() {
    User noRole = new User(UUID.randomUUID(), "norole@example.com", null, "Имя", "Фамилия", true);
    robot.interact(
        () -> {
          stubRole(noRole, false, false);
          loadUsersPage();
        });

    Button resources = robot.lookup("#resourcesButton").queryAs(Button.class);
    assertFalse(resources.isVisible());
    assertFalse(resources.isManaged());
  }

  // ===== Состояние при переходе =====

  @Test
  @Story("Переход между списками")
  @Tag("role:admin")
  @DisplayName("Новый список ресурсов чист: фильтры, смещение и ошибка сброшены")
  void freshResourcePageResetsState() {
    when(resourceService.search(any(), anyLong(), anyLong(), anyString(), anyString()))
        .thenThrow(new ServiceException("Сервер недоступен"));
    robot.interact(this::loadResourcePage);
    waitForResourceLoad();
    Label errorBefore = robot.lookup("#errorLabel").queryAs(Label.class);
    assertTrue(errorBefore.isVisible(), "ожидалась видимая ошибка перед переходом");
    robot.interact(
        () -> {
          textField("#searchField").setText("Отчёт");
          textField("#descriptionField").setText("годовой");
          button("#nextButton").getOnAction().handle(new ActionEvent());
        });

    ResourceListController before = resourceController;
    doReturn(List.of(resource("Документ 1")))
        .when(resourceService)
        .search(any(), anyLong(), anyLong(), anyString(), anyString());
    robot.interact(this::loadResourcePage);
    waitForResourceLoad();

    assertNotSame(before, resourceController, "переход должен создать новый контроллер");
    assertEquals("", textField("#searchField").getText());
    assertEquals("", textField("#descriptionField").getText());
    Label errorAfter = robot.lookup("#errorLabel").queryAs(Label.class);
    assertFalse(errorAfter.isVisible(), "ошибка не должна перетекать на новую страницу");
    assertEquals("", errorAfter.getText());
    assertEquals("Показано 1–1", textOf("#pageInfoLabel"));
  }

  @Test
  @Story("Переход между списками")
  @Tag("role:admin")
  @DisplayName("Новый список пользователей чист: фильтр, смещение и ошибка сброшены")
  void freshUsersPageResetsState() {
    when(usersService.search(any(), anyLong(), anyLong()))
        .thenThrow(new ServiceException("Сервер недоступен"));
    robot.interact(this::loadUsersPage);
    waitForUsersLoad();
    Label errorBefore = robot.lookup("#errorLabel").queryAs(Label.class);
    assertTrue(errorBefore.isVisible(), "ожидалась видимая ошибка перед переходом");
    robot.interact(
        () -> {
          textField("#searchField").setText("admin");
          button("#nextButton").getOnAction().handle(new ActionEvent());
        });

    UsersListController before = usersController;
    doReturn(List.of(ADMIN)).when(usersService).search(any(), anyLong(), anyLong());
    robot.interact(this::loadUsersPage);
    waitForUsersLoad();

    assertNotSame(before, usersController, "переход должен создать новый контроллер");
    assertEquals("", textField("#searchField").getText());
    Label errorAfter = robot.lookup("#errorLabel").queryAs(Label.class);
    assertFalse(errorAfter.isVisible(), "ошибка не должна перетекать на новую страницу");
    assertEquals("", errorAfter.getText());
    assertEquals("Показано 1–1", textOf("#pageInfoLabel"));
  }

  // ===== helpers =====

  private Resource resource(String title) {
    return new Resource(
        UUID.randomUUID(),
        title,
        "Описание",
        LocalDateTime.of(2024, 5, 6, 7, 8),
        LocalDateTime.of(2024, 5, 7, 9, 10),
        List.of());
  }

  /**
   * Жмёт кнопку напрямую через обработчик: синтетические клики ненадёжны под headless Monocle, а
   * привязка {@code onAction} в FXML — однострочная декларация.
   */
  private void fireButton(String query) {
    robot.interact(() -> button(query).getOnAction().handle(new ActionEvent()));
  }

  private Button button(String query) {
    return robot.lookup(query).queryAs(Button.class);
  }

  private TextField textField(String query) {
    return robot.lookup(query).queryAs(TextField.class);
  }

  private String textOf(String query) {
    return robot.lookup(query).queryAs(Label.class).getText();
  }

  /** Контроллер грузит данные в фоне; ждём, пока статус очистится. */
  private void waitForResourceLoad() {
    try {
      WaitForAsyncUtils.waitFor(
          30,
          TimeUnit.SECONDS,
          () -> {
            try {
              return textOf("#statusLabel").isEmpty();
            } catch (Exception e) {
              return false;
            }
          });
    } catch (TimeoutException e) {
      throw new AssertionError("Не дождались загрузки списка ресурсов", e);
    }
  }

  /** На списке пользователей успех без поиска тоже очищает статус. */
  private void waitForUsersLoad() {
    try {
      WaitForAsyncUtils.waitFor(
          30,
          TimeUnit.SECONDS,
          () -> {
            try {
              return !"Загрузка…".equals(textOf("#statusLabel"));
            } catch (Exception e) {
              return false;
            }
          });
    } catch (TimeoutException e) {
      throw new AssertionError("Не дождались загрузки списка пользователей", e);
    }
  }
}
