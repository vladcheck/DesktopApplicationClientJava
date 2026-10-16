package com.DesktopApplicationClientJava.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.util.WaitForAsyncUtils;

class ResourceListControllerTest extends FxTestBase {

  private static final User ADMIN =
      new User(UUID.randomUUID(), "admin@example.com", Role.ADMIN, "Имя", "Фамилия", true);

  private AuthService authService;
  private ResourceService resourceService;
  private ServiceRegistry services;
  private Navigator navigator;
  private TestableListController controller;
  private Stage stage;

  static class TestableListController extends ResourceListController {
    boolean confirmDeleteResult = false;
    boolean stubConfirm = true;
    int confirmCalls = 0;

    @Override
    protected boolean confirmDelete(Resource resource) {
      confirmCalls++;
      if (stubConfirm) {
        return confirmDeleteResult;
      }
      return super.confirmDelete(resource);
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
      controller = new TestableListController();
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
  void showsCurrentUserInHeader() {
    assertEquals("Имя Фамилия · ADMIN", textOf(robot, "#userInfoLabel"));
  }

  @Test
  void adminSeesCreateButton() {
    Button create = robot.lookup("#createButton").queryAs(Button.class);
    assertTrue(create.isVisible());
    assertTrue(create.isManaged());
  }

  @Test
  void moderatorSeesCreateButton() {
    robot.interact(
        () -> {
          when(authService.isAdmin()).thenReturn(false);
          when(authService.isModerator()).thenReturn(true);
          loadPage();
        });

    assertTrue(robot.lookup("#createButton").queryAs(Button.class).isVisible());
  }

  @Test
  void plainUserDoesNotSeeCreateButton() {
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
  void initialOpenLoadsFirstPageWithoutInteraction() {
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
  void sortDefaultsToNewestFirst() {
    @SuppressWarnings("unchecked")
    ComboBox<String> sort = robot.lookup("#sortCombo").queryAs(ComboBox.class);
    assertEquals(
        List.of("Сначала новые", "Сначала старые", "По названию (А-Я)", "По названию (Я-А)"),
        sort.getItems());
    assertEquals("Сначала новые", sort.getValue());
  }

  @Test
  void searchLoadsMatchingRows() {
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
  void changingSortReloadsWithTitleAsc() {
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
  void nextPageLoadsWithOffsetAndDisablesButtonsOnShortPage() {
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
  void serviceErrorShowsErrorAndClearsTable() {
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
  void logoutCallsServiceAndNavigatesToLogin() {
    robot.clickOn("#logoutButton");

    verify(authService).logout();
    verify(navigator).goTo("/fxml/LoginForm.fxml");
  }

  @Test
  void doubleClickRowOpensDetails() {
    Resource resource = resource("Документ 1");
    when(resourceService.search(any(), anyLong(), anyLong(), anyString(), anyString()))
        .thenReturn(List.of(resource));

    fireSearch(robot, "");
    waitForLoadComplete(robot);
    robot.doubleClickOn("Документ 1");

    verify(navigator).goTo(eq("/fxml/ResourceDetails.fxml"), any());
  }

  @Test
  void searchSendsTitleAndDescriptionFilters() {
    when(resourceService.search(any(), anyLong(), anyLong(), anyString(), anyString()))
        .thenReturn(List.of(resource("Документ 1")));

    robot.interact(
        () -> {
          TextField title = robot.lookup("#searchField").queryAs(TextField.class);
          title.setText("Отчёт");
          robot.lookup("#descriptionField").queryAs(TextField.class).setText("годовой");
          title.getOnAction().handle(new ActionEvent());
        });
    waitForLoadComplete(robot);

    verify(resourceService)
        .search(
            argThat(f -> "Отчёт".equals(f.getTitle()) && "годовой".equals(f.getDescription())),
            eq(0L),
            eq(20L),
            eq("createdAt"),
            eq("desc"));
  }

  @Test
  void blankDescriptionSendsNullFilter() {
    when(resourceService.search(any(), anyLong(), anyLong(), anyString(), anyString()))
        .thenReturn(List.of(resource("Документ 1")));

    fireSearch(robot, "Документ");
    waitForLoadComplete(robot);

    verify(resourceService)
        .search(
            argThat(f -> "Документ".equals(f.getTitle()) && f.getDescription() == null),
            eq(0L),
            eq(20L),
            eq("createdAt"),
            eq("desc"));
  }

  @Test
  void deleteButtonHiddenFromPlainUser() {
    robot.interact(
        () -> {
          when(authService.isAdmin()).thenReturn(false);
          when(authService.isModerator()).thenReturn(false);
          loadPage();
        });

    Button delete = robot.lookup("#deleteButton").queryAs(Button.class);
    assertFalse(delete.isVisible());
    assertFalse(delete.isManaged());
  }

  @Test
  void deleteButtonDisabledWithoutSelectionForModerator() {
    robot.interact(
        () -> {
          when(authService.isAdmin()).thenReturn(false);
          when(authService.isModerator()).thenReturn(true);
          loadPage();
        });

    Button delete = robot.lookup("#deleteButton").queryAs(Button.class);
    assertTrue(delete.isVisible());
    assertTrue(delete.isDisabled());
  }

  @Test
  void selectingRowEnablesDeleteButton() {
    Resource resource = resource("Документ 1");
    when(resourceService.search(any(), anyLong(), anyLong(), anyString(), anyString()))
        .thenReturn(List.of(resource));

    fireSearch(robot, "");
    waitForLoadComplete(robot);
    selectFirstRow(robot);

    assertFalse(robot.lookup("#deleteButton").queryAs(Button.class).isDisabled());
  }

  @Test
  void confirmedDeleteCallsServiceAndReloads() {
    Resource resource = resource("Документ 1");
    when(resourceService.search(any(), anyLong(), anyLong(), anyString(), anyString()))
        .thenReturn(List.of(resource));

    fireSearch(robot, "");
    waitForLoadComplete(robot);
    fireDelete(true);
    waitForLoadComplete(robot);

    verify(resourceService).delete(resource.getUuid());
    verify(resourceService, atLeast(2))
        .search(any(ResourceFilter.class), eq(0L), eq(20L), eq("createdAt"), eq("desc"));
    assertFalse(robot.lookup("#errorLabel").queryAs(Label.class).isVisible());
  }

  @Test
  void cancelledDeleteDoesNotCallService() {
    Resource resource = resource("Документ 1");
    when(resourceService.search(any(), anyLong(), anyLong(), anyString(), anyString()))
        .thenReturn(List.of(resource));

    fireSearch(robot, "");
    waitForLoadComplete(robot);
    fireDelete(false);

    assertEquals(1, controller.confirmCalls);
    verify(resourceService, never()).delete(any());
  }

  @Test
  void deleteErrorShowsErrorLabel() {
    Resource resource = resource("Документ 1");
    when(resourceService.search(any(), anyLong(), anyLong(), anyString(), anyString()))
        .thenReturn(List.of(resource));
    org.mockito.Mockito.doThrow(new ServiceException("Удаление запрещено"))
        .when(resourceService)
        .delete(resource.getUuid());

    fireSearch(robot, "");
    waitForLoadComplete(robot);
    fireDelete(true);
    waitForLoadComplete(robot);

    Label error = robot.lookup("#errorLabel").queryAs(Label.class);
    assertTrue(error.isVisible());
    assertEquals("Удаление запрещено", error.getText());
  }

  @Test
  void moderatorDeleteClickOpensConfirmationModal() {
    givenModeratorPageWithResource(resource("Документ 1"));

    Thread clicker = clickDeleteNoWait();
    DialogPane dialog = waitForDialog(robot);

    assertTrue(dialog.getScene().getWindow().isShowing(), "модалка удаления не открылась");
    assertEquals("Удалить ресурс?", dialog.getHeaderText());
    assertEquals(
        "«Документ 1» будет удалён без возможности восстановления.", dialog.getContentText());

    pressDialogButton(robot, dialog, ButtonType.CANCEL);
    waitForClicker(clicker);

    assertModalClosed(robot);
    assertEquals(1, controller.confirmCalls);
    verify(resourceService, never()).delete(any());
    assertEquals(1, rows(robot).size());
  }

  @Test
  void confirmingModalClosesItAndDeletesResource() {
    Resource resource = resource("Документ 1");
    givenModeratorPageWithResource(resource);

    Thread clicker = clickDeleteNoWait();
    DialogPane dialog = waitForDialog(robot);
    pressDialogButton(robot, dialog, ButtonType.OK);
    waitForClicker(clicker);

    assertModalClosed(robot);
    waitForLoadComplete(robot);

    verify(resourceService).delete(resource.getUuid());
  }

  @Test
  void deletingGoneResourceShowsErrorThroughModal() {
    Resource resource = resource("Документ 1");
    when(resourceService.search(any(), anyLong(), anyLong(), anyString(), anyString()))
        .thenReturn(List.of(resource));
    doThrow(new ServiceException("Ресурс не найден"))
        .when(resourceService)
        .delete(resource.getUuid());

    fireSearch(robot, "");
    waitForLoadComplete(robot);
    robot.interact(() -> controller.stubConfirm = false);
    selectFirstRow(robot);

    Thread clicker = clickDeleteNoWait();
    DialogPane dialog = waitForDialog(robot);
    pressDialogButton(robot, dialog, ButtonType.OK);
    waitForClicker(clicker);
    waitForLoadComplete(robot);

    assertModalClosed(robot);
    Label error = robot.lookup("#errorLabel").queryAs(Label.class);
    assertTrue(error.isVisible());
    assertEquals("Ресурс не найден", error.getText());
    assertEquals(1, rows(robot).size());
  }

  /**
   * Дано: пользователь с ролью МОДЕР на странице просмотра ресурсов, в списке есть хотя бы один
   * ресурс. Настоящий диалог подтверждения включён ({@code stubConfirm = false}), первая строка
   * выбрана — страница готова к клику по удалению.
   */
  private void givenModeratorPageWithResource(Resource resource) {
    when(resourceService.search(any(), anyLong(), anyLong(), anyString(), anyString()))
        .thenReturn(List.of(resource));
    robot.interact(
        () -> {
          when(authService.isAdmin()).thenReturn(false);
          when(authService.isModerator()).thenReturn(true);
          loadPage();
          controller.stubConfirm = false;
        });
    waitForLoadComplete(robot);
    selectFirstRow(robot);
  }

  private static void assertModalClosed(FxRobot robot) {
    assertTrue(robot.lookup(".dialog-pane").tryQuery().isEmpty(), "модалка удаления не закрылась");
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

  /**
   * Selects the first row and fires the delete button action directly: the confirmation {@code
   * Alert} is stubbed through {@link TestableListController#confirmDeleteResult}, because a real
   * modal dialog blocks headless Monocle.
   */
  private void fireDelete(boolean confirm) {
    controller.confirmDeleteResult = confirm;
    selectFirstRow(robot);
    robot.interact(
        () ->
            robot
                .lookup("#deleteButton")
                .queryAs(Button.class)
                .getOnAction()
                .handle(new ActionEvent()));
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
  private Thread clickDeleteNoWait() {
    Button delete = robot.lookup("#deleteButton").queryAs(Button.class);
    Thread clicker = new Thread(() -> robot.interact(delete::fire), "delete-click");
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
