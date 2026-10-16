package com.DesktopApplicationClientJava.ui.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;

import com.DesktopApplicationClientJava.Api.ApiClient;
import com.DesktopApplicationClientJava.controllers.ResourceListController;
import com.DesktopApplicationClientJava.entities.Resource;
import com.DesktopApplicationClientJava.navigation.Navigator;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.services.ServiceRegistry;
import com.DesktopApplicationClientJava.services.filter.ResourceFilter;
import com.DesktopApplicationClientJava.services.impl.AuthServiceImpl;
import com.DesktopApplicationClientJava.services.impl.ResourceServiceImpl;
import com.DesktopApplicationClientJava.session.Session;
import com.DesktopApplicationClientJava.ui.FxTestBase;
import com.DesktopApplicationClientJava.utils.ControllerWiring;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.util.WaitForAsyncUtils;

/**
 * UI-level integration tests for the resource list page: real services with a real ApiClient
 * against a live backend, no mocks (except Navigator).
 *
 * <p>Requires a running backend at {@code localhost:8080} (see README) and {@code
 * IT_ADMIN_PASSWORD} in {@code .env} for the mutating tests; otherwise tests abort. Excluded from
 * the default build and CI (see surefire {@code excludedGroups} in pom.xml). Run explicitly: {@code
 * mvn test -Dgroups=integration -DexcludedGroups= -Dtest=ResourceListIntegrationTest}.
 */
class ResourceListIntegrationTest extends IntegrationTestCase {

  private final FxRobot robot = new FxRobot();
  private Navigator navigator;
  private Stage stage;
  private Session session;
  private ServiceRegistry services;

  @BeforeAll
  static void initToolkit() throws Exception {
    FxTestBase.startSharedToolkit();
  }

  @AfterAll
  static void cleanupToolkit() throws Exception {
    FxTestBase.stopSharedToolkit(new FxRobot());
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

  void start(Stage stage) {
    this.stage = stage;
    this.navigator = mock(Navigator.class);
    stage.setScene(new Scene(new StackPane()));
    stage.show();
  }

  @Override
  @BeforeEach
  void setUpServices() {
    assumeTrue(isBackendUp(), "Backend is not running at localhost:8080, skipping");
    session = new Session();
    ApiClient apiClient = new ApiClient(session);
    authService = new AuthServiceImpl(apiClient, session);
    resourceService = new ResourceServiceImpl(apiClient);
    services = new ServiceRegistry(apiClient, session);
  }

  @Test
  void emptySearchShowsPlaceholderAndZeroCounter() {
    loginAsAdmin();
    loadListPage(robot);

    searchFor(robot, "IT NoSuchResource " + UUID.randomUUID());
    waitForLoadComplete(robot);

    assertTrue(rows(robot).isEmpty());
    assertEquals("Показано 0–0", textOf(robot, "#pageInfoLabel"));
    assertFalse(robot.lookup("#errorLabel").queryAs(Label.class).isVisible());
  }

  @Test
  void seededResourceAppearsInSearch() {
    loginAsAdmin();
    String title = "IT ResourceList " + UUID.randomUUID();
    Resource created = resourceService.create(title, "created by ResourceListIntegrationTest");
    try {
      loadListPage(robot);

      searchFor(robot, title);
      waitForLoadComplete(robot);

      List<Resource> rows = rows(robot);
      assertEquals(1, rows.size(), () -> "diagnostics: " + diagnostics(robot));
      assertEquals(title, rows.get(0).getTitle());
      assertEquals("Показано 1–1", textOf(robot, "#pageInfoLabel"));
    } finally {
      deleteQuietly(created.getUuid());
    }
  }

  @Test
  void sortByTitleAscOrdersSeededResources() {
    loginAsAdmin();
    String prefix = "IT ResourceSort " + UUID.randomUUID() + " ";
    Resource second = resourceService.create(prefix + "ZZZ", "seeded second");
    Resource first = resourceService.create(prefix + "AAA", "seeded first");
    try {
      loadListPage(robot);

      searchFor(robot, prefix);
      waitForLoadComplete(robot);

      selectSortAndReload(robot, "По названию (А–Я)");
      waitForLoadComplete(robot);

      List<Resource> rows = rows(robot);
      assertEquals(2, rows.size());
      assertEquals(prefix + "AAA", rows.get(0).getTitle());
      assertEquals(prefix + "ZZZ", rows.get(1).getTitle());
    } finally {
      deleteQuietly(first.getUuid());
      deleteQuietly(second.getUuid());
    }
  }

  @Test
  void unauthenticatedSearchShowsErrorAndLeavesTableEmpty() {
    // Fresh session, no login: the request cannot be authenticated.
    session.clear();
    loadListPage(robot);

    searchFor(robot, "IT ResourceList");
    waitForLoadComplete(robot);

    Label error = robot.lookup("#errorLabel").queryAs(Label.class);
    assertTrue(error.isVisible());
    assertFalse(error.getText().isBlank());
    assertTrue(rows(robot).isEmpty());
  }

  /**
   * The backend allowlists sort fields and silently falls back to {@code createdAt desc} for
   * anything else (see {@code createSort} in the backend's {@code ResourceService}) instead of
   * returning 5xx, so a true HTTP 500 is not triggerable through this endpoint. This test locks in
   * that lenient contract: unknown sort fields must still return 200 ordered by creation time.
   */
  @Test
  void invalidSortFieldFallsBackToCreatedAtDesc() {
    loginAsAdmin();
    String prefix = "IT SortFallback " + UUID.randomUUID() + " ";
    Resource older = resourceService.create(prefix + "older", "seeded first");
    Resource newer = resourceService.create(prefix + "newer", "seeded second");
    try {
      List<Resource> found =
          resourceService.search(new ResourceFilter(prefix, null), 0, 20, "noSuchField", "desc");

      assertEquals(2, found.size());
      assertEquals(newer.getUuid(), found.get(0).getUuid());
      assertEquals(older.getUuid(), found.get(1).getUuid());
    } finally {
      deleteQuietly(older.getUuid());
      deleteQuietly(newer.getUuid());
    }
  }

  /**
   * Полный цикл удаления глазами пользователя с правами МОДЕР и выше (вход как ADMIN, который по
   * иерархии ролей включает права МОДЕР): ресурс виден в списке, клик по «Удалить» открывает
   * модалку, подтверждение закрывает её и удаляет ресурс на бэкенде.
   */
  @Test
  void deleteCycleRemovesResourceFromBackend() {
    loginAsAdmin();
    String title = "IT ResourceDelete " + UUID.randomUUID();
    Resource created = resourceService.create(title, "created by ResourceListIntegrationTest");
    try {
      loadListPage(robot);
      // Дождаться начальной загрузки (пустой фильтр), иначе она может финишировать позже поиска
      // и перетереть таблицу полным списком.
      waitForLoadComplete(robot);

      searchFor(robot, title);
      waitForLoadComplete(robot);
      assertEquals(1, rows(robot).size(), () -> "diagnostics: " + diagnostics(robot));

      selectFirstRow(robot);

      Thread clicker = clickDeleteNoWait(robot);
      DialogPane dialog = waitForDialog(robot);
      assertTrue(dialog.getScene().getWindow().isShowing(), "модалка удаления не открылась");
      pressDialogButton(robot, dialog, ButtonType.OK);
      waitForClicker(clicker);
      waitForLoadComplete(robot);

      assertTrue(
          robot.lookup(".dialog-pane").tryQuery().isEmpty(), "модалка удаления не закрылась");
      assertTrue(rows(robot).isEmpty());
      assertEquals("Показано 0–0", textOf(robot, "#pageInfoLabel"));
      assertThrows(ServiceException.class, () -> resourceService.getById(created.getUuid()));
    } finally {
      deleteQuietly(created.getUuid());
    }
  }

  private void loginAsAdmin() {
    assumeTrue(
        adminPassword != null && !adminPassword.isBlank(),
        "IT_ADMIN_PASSWORD is not set, skipping");
    authService.login(adminEmail, adminPassword);
  }

  private void loadListPage(FxRobot robot) {
    robot.interact(
        () -> {
          try {
            ResourceListController controller = new ResourceListController();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ResourceList.fxml"));
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
        });
  }

  /**
   * Fills the search field and fires its action directly: synthetic Enter key delivery to a
   * TextField is unreliable under headless Monocle (silently eaten, no load triggered), while the
   * FXML {@code onAction} binding itself is a one-line declaration. The async controller → service
   * → backend → table path under test is fully exercised either way.
   */
  private static void searchFor(FxRobot robot, String query) {
    robot.interact(
        () -> {
          TextField field = robot.lookup("#searchField").queryAs(TextField.class);
          field.setText(query);
          field.getOnAction().handle(new ActionEvent());
        });
    assertEquals(
        query,
        robot.lookup("#searchField").queryAs(TextField.class).getText(),
        "search field did not take the query");
  }

  /**
   * Selects a sort option programmatically: ComboBox popup clicks are unreliable under headless
   * Monocle. Firing the search action afterwards triggers the same {@code loadAsync} path with the
   * newly selected sort, which is what the test needs.
   */
  @SuppressWarnings("unchecked")
  private static void selectSortAndReload(FxRobot robot, String option) {
    robot.interact(
        () -> {
          robot.lookup("#sortCombo").queryAs(ComboBox.class).getSelectionModel().select(option);
          TextField field = robot.lookup("#searchField").queryAs(TextField.class);
          field.getOnAction().handle(new ActionEvent());
        });
  }

  /** The controller loads data on a background thread; wait until the status clears. */
  private static void waitForLoadComplete(FxRobot robot) {
    try {
      WaitForAsyncUtils.waitFor(
          30,
          TimeUnit.SECONDS,
          () -> {
            try {
              return robot.lookup("#statusLabel").queryAs(Label.class).getText().isEmpty();
            } catch (Exception e) {
              return false;
            }
          });
    } catch (TimeoutException e) {
      throw new AssertionError("Timed out waiting for resource list to load", e);
    }
  }

  @SuppressWarnings("unchecked")
  private static List<Resource> rows(FxRobot robot) {
    return robot.lookup("#resourceTable").queryAs(TableView.class).getItems();
  }

  @SuppressWarnings("unchecked")
  private static void selectFirstRow(FxRobot robot) {
    robot.interact(
        () ->
            robot.lookup("#resourceTable").queryAs(TableView.class).getSelectionModel().select(0));
  }

  /**
   * Нажимает кнопку удаления, не дожидаясь закрытия модального окна: настоящий {@code Alert}
   * блокирует поток JavaFX через {@code showAndWait}, поэтому клик уходит в отдельный поток, а тест
   * продолжает работать с диалогом.
   */
  private static Thread clickDeleteNoWait(FxRobot robot) {
    Button delete = robot.lookup("#deleteButton").queryAs(Button.class);
    Thread clicker = new Thread(() -> robot.interact(delete::fire), "it-delete-click");
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

  private static void pressDialogButton(FxRobot robot, DialogPane dialog, ButtonType type) {
    robot.interact(() -> ((Button) dialog.lookupButton(type)).fire());
  }

  private static void waitForClicker(Thread clicker) {
    try {
      WaitForAsyncUtils.waitFor(30, TimeUnit.SECONDS, () -> !clicker.isAlive());
    } catch (TimeoutException e) {
      throw new AssertionError("Клик по удалению не завершился после закрытия диалога", e);
    }
  }

  private static String textOf(FxRobot robot, String query) {
    return robot.lookup(query).queryAs(Label.class).getText();
  }

  private static String diagnostics(FxRobot robot) {
    Label error = robot.lookup("#errorLabel").queryAs(Label.class);
    return "pageInfo='"
        + textOf(robot, "#pageInfoLabel")
        + "' status='"
        + textOf(robot, "#statusLabel")
        + "' errorVisible="
        + error.isVisible()
        + " error='"
        + error.getText()
        + "' searchField='"
        + robot.lookup("#searchField").queryAs(TextField.class).getText()
        + "'";
  }

  private void deleteQuietly(UUID id) {
    try {
      resourceService.delete(id);
    } catch (Exception ignored) {
      // Best-effort cleanup, never fail the test.
    }
  }
}
