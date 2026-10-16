package com.DesktopApplicationClientJava.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.DesktopApplicationClientJava.controllers.UsersListController;
import com.DesktopApplicationClientJava.entities.Role;
import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.navigation.Navigator;
import com.DesktopApplicationClientJava.services.AuthService;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.services.ServiceRegistry;
import com.DesktopApplicationClientJava.services.UsersService;
import com.DesktopApplicationClientJava.services.filter.UserFilter;
import com.DesktopApplicationClientJava.session.Session;
import com.DesktopApplicationClientJava.utils.ControllerWiring;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxToolkit;
import org.testfx.util.WaitForAsyncUtils;

@Epic("Пользователи")
@Feature("Список пользователей")
@DisplayName("Список пользователей")
class UsersListControllerTest extends FxTestBase {

  private static final User ADMIN =
      new User(UUID.randomUUID(), "admin@example.com", Role.ADMIN, "Имя", "Фамилия", true);

  private AuthService authService;
  private UsersService usersService;
  private ServiceRegistry services;
  private Navigator navigator;
  private Session session;
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

  void start(Stage stage) throws Exception {
    authService = mock(AuthService.class);
    usersService = mock(UsersService.class);
    services = mock(ServiceRegistry.class);
    when(services.getAuthService()).thenReturn(authService);
    when(services.getUsersService()).thenReturn(usersService);
    navigator = mock(Navigator.class);
    session = new Session();
    session.setCurrentUser(ADMIN);
    session.setAccessToken("access");
    session.setRefreshToken("refresh");
    this.stage = stage;
    stubAdmin();
    loadPage();
  }

  private void stubAdmin() {
    when(authService.getCurrentUser()).thenReturn(ADMIN);
    when(authService.isAdmin()).thenReturn(true);
  }

  private void loadPage() {
    try {
      UsersListController controller = new UsersListController();
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserList.fxml"));
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
  @DisplayName("Шапка показывает текущего пользователя")
  void showsCurrentUserInHeader() {
    assertEquals("Имя Фамилия · ADMIN", textOf("#userInfoLabel"));
  }

  @Test
  @DisplayName("Гость без пользователя не роняет страницу")
  void guestWithoutUserDoesNotThrow() {
    robot.interact(
        () -> {
          when(authService.getCurrentUser()).thenReturn(null);
          when(authService.isAdmin()).thenReturn(false);
          loadPage();
        });
    waitForLoadComplete();

    assertEquals("", textOf("#userInfoLabel"));
    Button create = robot.lookup("#createButton").queryAs(Button.class);
    assertFalse(create.isVisible());
    assertFalse(create.isManaged());
  }

  @Test
  @DisplayName("Администратор видит кнопку создания")
  void adminSeesCreateButton() {
    Button create = robot.lookup("#createButton").queryAs(Button.class);
    assertTrue(create.isVisible());
    assertTrue(create.isManaged());
  }

  @Test
  @DisplayName("Не администратор не видит кнопку создания")
  void nonAdminDoesNotSeeCreateButton() {
    robot.interact(
        () -> {
          when(authService.isAdmin()).thenReturn(false);
          loadPage();
        });

    Button create = robot.lookup("#createButton").queryAs(Button.class);
    assertFalse(create.isVisible());
    assertFalse(create.isManaged());
  }

  @Test
  @DisplayName("Администратор видит кнопку перехода к документам")
  void adminSeesResourcesButton() {
    Button resources = robot.lookup("#resourcesButton").queryAs(Button.class);
    assertTrue(resources.isVisible());
    assertTrue(resources.isManaged());
  }

  @Test
  @DisplayName("Не администратор не видит кнопку перехода к документам")
  void nonAdminDoesNotSeeResourcesButton() {
    robot.interact(
        () -> {
          when(authService.isAdmin()).thenReturn(false);
          loadPage();
        });

    Button resources = robot.lookup("#resourcesButton").queryAs(Button.class);
    assertFalse(resources.isVisible());
    assertFalse(resources.isManaged());
  }

  @Test
  @DisplayName("Сортировка по умолчанию — по email от А к Я")
  void sortDefaultsToEmailAsc() {
    @SuppressWarnings("unchecked")
    ComboBox<String> sort = robot.lookup("#sortCombo").queryAs(ComboBox.class);
    assertEquals(
        List.of("По email (А-Я)", "По email (Я-А)", "По имени (А-Я)", "По фамилии (А-Я)"),
        sort.getItems());
    assertEquals("По email (А-Я)", sort.getValue());
  }

  @Test
  @DisplayName("Открытие страницы сразу грузит первую страницу")
  void initialOpenLoadsFirstPageWithoutInteraction() {
    when(usersService.search(any(), eq(0L), eq(20L))).thenReturn(List.of(user("a@example.com")));

    robot.interact(this::loadPage);
    waitForLoadComplete();

    verify(usersService, atLeastOnce())
        .search(argThat(UsersListControllerTest::isEmptyFilter), eq(0L), eq(20L));
    assertEquals(1, rows().size());
    assertEquals("Показано 1–1", textOf("#pageInfoLabel"));
  }

  @Test
  @DisplayName("Пустая таблица показывает плейсхолдер")
  void emptyTableShowsPlaceholder() {
    when(usersService.search(any(), eq(0L), eq(20L))).thenReturn(List.of());
    robot.interact(this::loadPage);
    waitForLoadComplete();

    TableView<User> table = robot.lookup("#usersTable").queryAs(TableView.class);
    assertTrue(table.getItems().isEmpty());
    assertTrue(table.getPlaceholder() instanceof Label);
    assertEquals("Пользователей пока нет", ((Label) table.getPlaceholder()).getText());
  }

  @Test
  @DisplayName("Следующая страница грузится со смещением 20")
  void nextPageLoadsWithOffset20() {
    when(usersService.search(any(), eq(0L), eq(20L))).thenReturn(fullPage());
    when(usersService.search(any(), eq(20L), eq(20L))).thenReturn(List.of(user("z@example.com")));

    fireSearch("");
    waitForLoadComplete();
    fireNext();
    waitForLoadComplete();

    verify(usersService).search(any(UserFilter.class), eq(20L), eq(20L));
    assertEquals("Показано 21–21", textOf("#pageInfoLabel"));
    assertFalse(robot.lookup("#prevButton").queryAs(Button.class).isDisabled());
  }

  @Test
  @DisplayName("Короткая страница блокирует кнопку вперёд")
  void shortPageDisablesNext() {
    when(usersService.search(any(), eq(0L), eq(20L))).thenReturn(List.of(user("a@example.com")));

    fireSearch("");
    waitForLoadComplete();

    assertEquals("Показано 1–1", textOf("#pageInfoLabel"));
    assertTrue(robot.lookup("#nextButton").queryAs(Button.class).isDisabled());
    assertTrue(robot.lookup("#prevButton").queryAs(Button.class).isDisabled());
  }

  @Test
  @DisplayName("Пустая страница блокирует пагинацию")
  void emptyPageDisablesPagination() {
    when(usersService.search(any(), eq(0L), eq(20L))).thenReturn(List.of());

    fireSearch("");
    waitForLoadComplete();

    assertEquals("Показано 0–0", textOf("#pageInfoLabel"));
    assertTrue(robot.lookup("#nextButton").queryAs(Button.class).isDisabled());
    assertTrue(robot.lookup("#prevButton").queryAs(Button.class).isDisabled());
  }

  @Test
  @DisplayName("Кнопка назад не уводит смещение в минус")
  void prevDoesNotGoNegative() {
    when(usersService.search(any(), eq(0L), eq(20L))).thenReturn(List.of(user("a@example.com")));

    fireSearch("");
    waitForLoadComplete();
    firePrev();
    waitForLoadComplete();

    verify(usersService, atLeastOnce()).search(any(UserFilter.class), eq(0L), eq(20L));
    assertEquals("Показано 1–1", textOf("#pageInfoLabel"));
    assertTrue(robot.lookup("#prevButton").queryAs(Button.class).isDisabled());
  }

  @Test
  @DisplayName("Null от бэкенда считается пустой страницей")
  void nullBackendTreatedAsEmpty() {
    when(usersService.search(any(), eq(0L), eq(20L))).thenReturn(null);

    fireSearch("");
    waitForLoadComplete();

    assertTrue(rows().isEmpty());
    assertEquals("Показано 0–0", textOf("#pageInfoLabel"));
    assertFalse(robot.lookup("#errorLabel").queryAs(Label.class).isVisible());
  }

  @Test
  @DisplayName("Поиск по email отправляет email-фильтр на сервер")
  void searchByEmailSendsEmailFilter() {
    when(usersService.search(any(), eq(0L), eq(500L)))
        .thenReturn(List.of(user("ivan@example.com", "Иван", "Петров")));

    fireSearch("ivan@example.com");
    waitForLoadComplete();

    verify(usersService)
        .search(argThat(f -> "ivan@example.com".equals(f.getEmail())), eq(0L), eq(500L));
    assertEquals(1, rows().size());
    assertEquals("Показано 1", textOf("#pageInfoLabel"));
  }

  @Test
  @DisplayName("Поиск по имени фильтруется локально с пустым серверным фильтром")
  void searchByNameFiltersLocally() {
    when(usersService.search(any(), eq(0L), eq(500L)))
        .thenReturn(
            List.of(
                user("ivan@example.com", "Иван", "Петров"),
                user("anna@example.com", "Анна", "Сидорова")));

    fireSearch("Иван");
    waitForLoadComplete();

    verify(usersService).search(argThat(UsersListControllerTest::isEmptyFilter), eq(0L), eq(500L));
    assertEquals(1, rows().size());
    assertEquals("ivan@example.com", rows().get(0).getEmail());
  }

  @Test
  @DisplayName("Поиск нечувствителен к регистру")
  void searchIsCaseInsensitive() {
    when(usersService.search(any(), eq(0L), eq(500L)))
        .thenReturn(List.of(user("Ivan@Example.com", "Иван", "Петров")));

    fireSearch("иВаН");
    waitForLoadComplete();

    assertEquals(1, rows().size());
  }

  @Test
  @DisplayName("Пробелы в поиске считаются отсутствием поиска")
  void blankQueryTreatedAsNoSearch() {
    when(usersService.search(any(), eq(0L), eq(20L))).thenReturn(fullPage());

    fireSearch("   ");
    waitForLoadComplete();

    verify(usersService, atLeastOnce())
        .search(argThat(UsersListControllerTest::isEmptyFilter), eq(0L), eq(20L));
    assertEquals("Показано 1–20", textOf("#pageInfoLabel"));
    assertFalse(robot.lookup("#nextButton").queryAs(Button.class).isDisabled());
  }

  @Test
  @DisplayName("Поиск сбрасывает смещение в ноль")
  void searchResetsOffset() {
    when(usersService.search(any(), eq(0L), eq(20L))).thenReturn(fullPage());
    when(usersService.search(any(), eq(20L), eq(20L))).thenReturn(fullPage());

    fireSearch("");
    waitForLoadComplete();
    fireNext();
    waitForLoadComplete();

    when(usersService.search(any(), eq(0L), eq(20L))).thenReturn(List.of(user("a@example.com")));
    fireSearch("");
    waitForLoadComplete();

    verify(usersService, atLeastOnce()).search(any(UserFilter.class), eq(0L), eq(20L));
    assertEquals("Показано 1–1", textOf("#pageInfoLabel"));
  }

  @Test
  @DisplayName("Null-поля пользователей не роняют поиск и сортировку")
  void nullFieldsDoNotCauseNpe() {
    User broken = new User(UUID.randomUUID(), null, null, null, null, null);
    when(usersService.search(any(), eq(0L), eq(500L)))
        .thenReturn(new ArrayList<>(List.of(broken, user("a@example.com", "А", "Б"))));

    fireSearch("a@example");
    waitForLoadComplete();

    assertEquals(1, rows().size());
    assertEquals("a@example.com", rows().get(0).getEmail());
  }

  @Test
  @DisplayName("Сортировка по email от А к Я")
  void sortEmailAsc() {
    when(usersService.search(any(), eq(0L), eq(20L)))
        .thenReturn(
            new ArrayList<>(
                List.of(user("b@example.com"), user("a@example.com"), user("c@example.com"))));

    fireSearch("");
    waitForLoadComplete();

    assertEquals(
        List.of("a@example.com", "b@example.com", "c@example.com"),
        rows().stream().map(User::getEmail).toList());
  }

  @Test
  @DisplayName("Сортировка по email от Я к А")
  void sortEmailDesc() {
    when(usersService.search(any(), eq(0L), eq(20L)))
        .thenReturn(new ArrayList<>(List.of(user("a@example.com"), user("c@example.com"))));

    selectSort(1);
    waitForLoadComplete();

    assertEquals(
        List.of("c@example.com", "a@example.com"), rows().stream().map(User::getEmail).toList());
    verify(usersService, atLeastOnce()).search(any(UserFilter.class), eq(0L), eq(20L));
  }

  @Test
  @DisplayName("Сортировка по имени")
  void sortByFirstName() {
    when(usersService.search(any(), eq(0L), eq(20L)))
        .thenReturn(
            new ArrayList<>(
                List.of(user("b@example.com", "Борис", "А"), user("a@example.com", "Анна", "Б"))));

    selectSort(2);
    waitForLoadComplete();

    assertEquals(List.of("Анна", "Борис"), rows().stream().map(User::getFirstName).toList());
  }

  @Test
  @DisplayName("Сортировка по фамилии, null email как пустая строка")
  void sortByLastNameAndNullEmailAsEmpty() {
    User noEmail = new User(UUID.randomUUID(), null, Role.USER, "Без", "Почты", true);
    when(usersService.search(any(), eq(0L), eq(20L)))
        .thenReturn(new ArrayList<>(List.of(user("b@example.com", "А", "Юрьев"), noEmail)));

    selectSort(3);
    waitForLoadComplete();
    assertEquals(2, rows().size());

    robot.interact(
        () -> robot.lookup("#sortCombo").queryAs(ComboBox.class).getSelectionModel().select(0));
    waitForLoadComplete();

    assertEquals(null, rows().get(0).getEmail());
    assertEquals("b@example.com", rows().get(1).getEmail());
  }

  @Test
  @DisplayName("Колонки показывают роль и активность Да и Нет")
  void columnsShowRoleAndEnabled() {
    User enabledAdmin = user("admin@example.com", "А", "Б", Role.ADMIN, true);
    User disabledNoRole = new User(UUID.randomUUID(), "x@example.com", null, "В", "Г", false);
    when(usersService.search(any(), eq(0L), eq(20L)))
        .thenReturn(new ArrayList<>(List.of(enabledAdmin, disabledNoRole)));

    fireSearch("");
    waitForLoadComplete();

    assertEquals("ADMIN", cellValue(3, enabledAdmin));
    assertEquals("", cellValue(3, disabledNoRole));
    assertEquals("Да", cellValue(4, enabledAdmin));
    assertEquals("Нет", cellValue(4, disabledNoRole));
  }

  @Test
  @DisplayName("Создание ведёт на форму создания")
  void createNavigatesToForm() {
    fireButton("#createButton");

    verify(navigator).goTo("/fxml/UserCreateForm.fxml");
  }

  @Test
  @DisplayName("Двойной клик открывает детали пользователя")
  void doubleClickRowOpensDetails() {
    User selected = user("detail@example.com", "Деталь", "Тест");
    when(usersService.search(any(), eq(0L), eq(20L))).thenReturn(List.of(selected));

    fireSearch("");
    waitForLoadComplete();
    robot.doubleClickOn("detail@example.com");

    verify(navigator).goTo(eq("/fxml/UserDetails.fxml"), any());
  }

  @Test
  @DisplayName("Выход вызывает сервис и ведёт на страницу входа")
  void logoutCallsServiceAndNavigatesToLogin() {
    fireButton("#logoutButton");

    verify(authService).logout();
    verify(navigator).goToLoginOnce();
    assertNull(session.getCurrentUser());
  }

  @Test
  @DisplayName("Кнопка документов ведёт к списку ресурсов")
  void resourcesButtonNavigatesToResourceList() {
    fireButton("#resourcesButton");

    verify(navigator).goTo("/fxml/ResourceList.fxml");
  }

  @Test
  @DisplayName("Ошибка сервиса показывает текст и очищает таблицу")
  void serviceErrorShowsErrorAndClearsTable() {
    when(usersService.search(any(), eq(0L), eq(20L)))
        .thenThrow(new ServiceException("Сервер недоступен"));

    fireSearch("");
    waitForLoadComplete();

    Label error = robot.lookup("#errorLabel").queryAs(Label.class);
    assertTrue(error.isVisible());
    assertEquals("Сервер недоступен", error.getText());
    assertTrue(rows().isEmpty());
    assertEquals("Показано 0–0", textOf("#pageInfoLabel"));
  }

  @Test
  @DisplayName("Неожиданная ошибка показывает общее сообщение")
  void runtimeExceptionShowsGenericMessage() {
    when(usersService.search(any(), eq(0L), eq(20L))).thenThrow(new RuntimeException("boom"));

    fireSearch("");
    waitForLoadComplete();

    Label error = robot.lookup("#errorLabel").queryAs(Label.class);
    assertTrue(error.isVisible());
    assertEquals("Не удалось загрузить список", error.getText());
    assertTrue(rows().isEmpty());
  }

  @Test
  @DisplayName("Ошибка 403 ведёт на вход с очищенной сессией")
  void forbiddenErrorRedirectsToLogin() {
    when(usersService.search(any(), eq(0L), eq(20L)))
        .thenThrow(new ServiceException("Доступ запрещён", 403));

    fireSearch("");
    awaitLoginRedirect();

    verify(navigator).goToLoginOnce();
    assertNull(session.getCurrentUser());
    assertNull(session.getAccessToken());
    assertFalse(robot.lookup("#errorLabel").queryAs(Label.class).isVisible());
  }

  @Test
  @DisplayName("Следующая загрузка очищает ошибку")
  void errorClearedOnNextLoad() {
    when(usersService.search(any(), eq(0L), eq(20L)))
        .thenThrow(new ServiceException("Сервер недоступен"));
    fireSearch("");
    waitForLoadComplete();
    assertTrue(robot.lookup("#errorLabel").queryAs(Label.class).isVisible());

    when(usersService.search(any(), eq(0L), eq(20L))).thenReturn(List.of(user("a@example.com")));
    fireSearch("");
    waitForLoadComplete();

    Label error = robot.lookup("#errorLabel").queryAs(Label.class);
    assertFalse(error.isVisible());
    assertEquals("", error.getText());
    assertEquals(1, rows().size());
  }

  private static boolean isEmptyFilter(UserFilter filter) {
    return filter != null
        && filter.getEmail() == null
        && filter.getRole() == null
        && filter.getFirstName() == null
        && filter.getLastName() == null
        && filter.getEnabled() == null;
  }

  private static User user(String email) {
    return user(email, "Имя", "Фамилия");
  }

  private static User user(String email, String firstName, String lastName) {
    return user(email, firstName, lastName, Role.USER, true);
  }

  private static User user(
      String email, String firstName, String lastName, Role role, Boolean enabled) {
    return new User(UUID.randomUUID(), email, role, firstName, lastName, enabled);
  }

  private static List<User> fullPage() {
    List<User> page = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      page.add(user(String.format("user%02d@example.com", i)));
    }
    return page;
  }

  private void fireSearch(String query) {
    fillText(robot, "#searchField", query);
    robot.interact(
        () ->
            robot
                .lookup("#searchField")
                .queryAs(TextField.class)
                .getOnAction()
                .handle(new ActionEvent()));
  }

  private void fireNext() {
    robot.interact(
        () ->
            robot
                .lookup("#nextButton")
                .queryAs(Button.class)
                .getOnAction()
                .handle(new ActionEvent()));
  }

  private void firePrev() {
    robot.interact(
        () ->
            robot
                .lookup("#prevButton")
                .queryAs(Button.class)
                .getOnAction()
                .handle(new ActionEvent()));
  }

  private void fireButton(String query) {
    robot.interact(
        () -> robot.lookup(query).queryAs(Button.class).getOnAction().handle(new ActionEvent()));
  }

  @SuppressWarnings("unchecked")
  private void selectSort(int index) {
    robot.interact(
        () -> robot.lookup("#sortCombo").queryAs(ComboBox.class).getSelectionModel().select(index));
  }

  private void waitForLoadComplete() {
    try {
      WaitForAsyncUtils.waitFor(
          30,
          TimeUnit.SECONDS,
          () -> {
            try {
              String status = robot.lookup("#statusLabel").queryAs(Label.class).getText();
              return !"Загрузка…".equals(status);
            } catch (Exception e) {
              return false;
            }
          });
    } catch (TimeoutException e) {
      throw new AssertionError("Не дождались загрузки списка пользователей", e);
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

  @SuppressWarnings("unchecked")
  private List<User> rows() {
    return robot.lookup("#usersTable").queryAs(TableView.class).getItems();
  }

  private String textOf(String query) {
    return robot.lookup(query).queryAs(Label.class).getText();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private String cellValue(int columnIndex, User user) {
    TableView<User> table = robot.lookup("#usersTable").queryAs(TableView.class);
    final TableColumn<User, String>[] holder = new TableColumn[1];
    robot.interact(
        () -> holder[0] = (TableColumn<User, String>) table.getColumns().get(columnIndex));
    TableColumn<User, String> column = holder[0];
    TableColumn.CellDataFeatures<User, String> features =
        new TableColumn.CellDataFeatures<>(table, column, user);
    final String[] value = new String[1];
    robot.interact(() -> value[0] = column.getCellValueFactory().call(features).getValue());
    return value[0];
  }
}
