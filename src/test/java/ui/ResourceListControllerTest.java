package ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.DesktopApplicationClientJava.controllers.ResourceListController;
import com.DesktopApplicationClientJava.entities.Resource;
import com.DesktopApplicationClientJava.entities.Role;
import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.navigation.Navigator;
import com.DesktopApplicationClientJava.services.AuthService;
import com.DesktopApplicationClientJava.services.ResourceService;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.services.ServiceRegistry;
import com.DesktopApplicationClientJava.services.filter.ResourceFilter;
import com.DesktopApplicationClientJava.session.Session;
import com.DesktopApplicationClientJava.utils.ControllerWiring;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

@ExtendWith(ApplicationExtension.class)
class ResourceListControllerTest {

  private static final User ADMIN =
      new User(UUID.randomUUID(), "admin@example.com", Role.ADMIN, "Имя", "Фамилия", true);

  private AuthService authService;
  private ResourceService resourceService;
  private ServiceRegistry services;
  private Navigator navigator;
  private ResourceListController controller;
  private Stage stage;

  @Start
  void start(Stage stage) throws Exception {
    authService = mock(AuthService.class);
    resourceService = mock(ResourceService.class);
    services = mock(ServiceRegistry.class);
    when(services.getAuthService()).thenReturn(authService);
    when(services.getResourceService()).thenReturn(resourceService);
    navigator = mock(Navigator.class);
    this.stage = stage;
    stubAdmin();
    loadPage();
  }

  private void stubAdmin() {
    when(authService.getCurrentUser()).thenReturn(ADMIN);
    when(authService.isAdmin()).thenReturn(true);
    when(authService.isModerator()).thenReturn(false);
  }

  private void loadPage() {
    try {
      controller = new ResourceListController();
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ResourceList.fxml"));
      loader.setControllerFactory(
          clazz -> {
            ControllerWiring.wire(controller, navigator, new Session(), services);
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
  void showsCurrentUserInHeader(FxRobot robot) {
    assertEquals("Имя Фамилия · ADMIN", textOf(robot, "#userInfoLabel"));
  }

  @Test
  void adminSeesCreateButton(FxRobot robot) {
    Button create = robot.lookup("#createButton").queryAs(Button.class);
    assertTrue(create.isVisible());
    assertTrue(create.isManaged());
  }

  @Test
  void moderatorSeesCreateButton(FxRobot robot) {
    robot.interact(
        () -> {
          when(authService.isAdmin()).thenReturn(false);
          when(authService.isModerator()).thenReturn(true);
          loadPage();
        });

    assertTrue(robot.lookup("#createButton").queryAs(Button.class).isVisible());
  }

  @Test
  void plainUserDoesNotSeeCreateButton(FxRobot robot) {
    robot.interact(
        () -> {
          when(authService.isAdmin()).thenReturn(false);
          when(authService.isModerator()).thenReturn(false);
          loadPage();
        });

    Button create = robot.lookup("#createButton").queryAs(Button.class);
    assertFalse(create.isVisible());
    assertFalse(create.isManaged());
  }

  @Test
  void initialOpenLoadsFirstPageWithoutInteraction(FxRobot robot) {
    // Regression test for the missing auto-load: opening the page must fetch the first page
    // with no user interaction. Red until initialize() triggers the initial load.
    when(resourceService.search(any(), anyLong(), anyLong(), anyString(), anyString()))
        .thenReturn(List.of(resource("Документ 1")));

    robot.interact(this::loadPage);

    waitForLoadComplete(robot);
    assertEquals(1, rows(robot).size(), "page opened empty: no initial load happened");
    assertEquals("Показано 1–1", textOf(robot, "#pageInfoLabel"));
  }

  @Test
  void sortDefaultsToNewestFirst(FxRobot robot) {

    ComboBox<String> sort = robot.lookup("#sortCombo").queryAs(ComboBox.class);
    assertEquals(
        List.of("Сначала новые", "Сначала старые", "По названию (А-Я)", "По названию (Я-А)"),
        sort.getItems());
    assertEquals("Сначала новые", sort.getValue());
  }

  @Test
  void searchLoadsMatchingRows(FxRobot robot) {
    when(resourceService.search(any(), anyLong(), anyLong(), anyString(), anyString()))
        .thenReturn(List.of(resource("Документ 1")));

    fireSearch(robot, "Документ");
    waitForLoadComplete(robot);

    verify(resourceService)
        .search(
            argThat(f -> "Документ".equals(f.getTitle())),
            eq(0L),
            eq(20L),
            eq("createdAt"),
            eq("desc"));
    assertEquals(1, rows(robot).size());
    assertEquals("Документ 1", rows(robot).get(0).getTitle());
    assertEquals("Показано 1–1", textOf(robot, "#pageInfoLabel"));
    assertFalse(robot.lookup("#errorLabel").queryAs(Label.class).isVisible());
  }

  @Test
  void changingSortReloadsWithTitleAsc(FxRobot robot) {
    when(resourceService.search(any(), anyLong(), anyLong(), anyString(), anyString()))
        .thenReturn(List.of());

    robot.interact(
        () -> robot.lookup("#sortCombo").queryAs(ComboBox.class).getSelectionModel().select(2));
    // NB: ComboBox fires onAction on programmatic selection too, so selecting already reloads
    // with the new sort — no extra search trigger needed.
    waitForLoadComplete(robot);

    verify(resourceService)
        .search(any(ResourceFilter.class), eq(0L), eq(20L), eq("title"), eq("asc"));
  }

  @Test
  void nextPageLoadsWithOffsetAndDisablesButtonsOnShortPage(FxRobot robot) {
    when(resourceService.search(any(), anyLong(), anyLong(), anyString(), anyString()))
        .thenReturn(List.of(resource("Документ 1")));

    fireSearch(robot, "");
    waitForLoadComplete(robot);
    // Fire directly: synthetic clicks are unreliable under headless Monocle (silently missed).
    robot.interact(
        () ->
            robot
                .lookup("#nextButton")
                .queryAs(Button.class)
                .getOnAction()
                .handle(new ActionEvent()));
    waitForLoadComplete(robot);

    verify(resourceService)
        .search(any(ResourceFilter.class), eq(20L), eq(20L), eq("createdAt"), eq("desc"));
    assertEquals("Показано 21–21", textOf(robot, "#pageInfoLabel"));
    assertFalse(robot.lookup("#prevButton").queryAs(Button.class).isDisabled());
    assertTrue(robot.lookup("#nextButton").queryAs(Button.class).isDisabled());
  }

  @Test
  void serviceErrorShowsErrorAndClearsTable(FxRobot robot) {
    when(resourceService.search(any(), anyLong(), anyLong(), anyString(), anyString()))
        .thenThrow(new ServiceException("Сервер недоступен"));

    fireSearch(robot, "");
    waitForLoadComplete(robot);

    Label error = robot.lookup("#errorLabel").queryAs(Label.class);
    assertTrue(error.isVisible());
    assertEquals("Сервер недоступен", error.getText());
    assertTrue(rows(robot).isEmpty());
    assertEquals("Показано 0–0", textOf(robot, "#pageInfoLabel"));
  }

  @Test
  void logoutCallsServiceAndNavigatesToLogin(FxRobot robot) {
    robot.clickOn("#logoutButton");

    verify(authService).logout();
    verify(navigator).goTo("/fxml/LoginForm.fxml");
  }

  @Test
  void doubleClickRowOpensDetails(FxRobot robot) {
    Resource resource = resource("Документ 1");
    when(resourceService.search(any(), anyLong(), anyLong(), anyString(), anyString()))
        .thenReturn(List.of(resource));

    fireSearch(robot, "");
    waitForLoadComplete(robot);
    robot.doubleClickOn("Документ 1");

    verify(navigator).goTo(eq("/fxml/ResourceDetails.fxml"), any());
  }

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
   * Fills the search field and fires its action directly: synthetic Enter key delivery to a
   * TextField is unreliable under headless Monocle (silently eaten, no load triggered), while the
   * FXML {@code onAction} binding itself is a one-line declaration. The async controller → service
   * → table path under test is fully exercised either way.
   */
  private static void fireSearch(FxRobot robot, String query) {
    robot.interact(
        () -> {
          TextField field = robot.lookup("#searchField").queryAs(TextField.class);
          field.setText(query);
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

  private static String textOf(FxRobot robot, String query) {
    return robot.lookup(query).queryAs(Label.class).getText();
  }
}
