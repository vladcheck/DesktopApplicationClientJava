package com.DesktopApplicationClientJava.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.DesktopApplicationClientJava.controllers.ResourceDetailsController;
import com.DesktopApplicationClientJava.entities.Resource;
import com.DesktopApplicationClientJava.navigation.Navigator;
import com.DesktopApplicationClientJava.services.AuthService;
import com.DesktopApplicationClientJava.services.FileService;
import com.DesktopApplicationClientJava.services.ResourceService;
import com.DesktopApplicationClientJava.services.ServiceRegistry;
import com.DesktopApplicationClientJava.session.Session;
import com.DesktopApplicationClientJava.utils.ControllerWiring;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.util.WaitForAsyncUtils;

/**
 * Компонентные тесты страницы деталей ресурса: удаление через настоящую модалку подтверждения (роль
 * МОДЕР).
 */
class ResourceDetailsControllerUiTest extends FxTestBase {

  private static final UUID RESOURCE_ID = UUID.randomUUID();

  private AuthService authService;
  private ResourceService resourceService;
  private ServiceRegistry services;
  private Navigator navigator;
  private TestableDetailsController controller;
  private Stage stage;

  static class TestableDetailsController extends ResourceDetailsController {
    boolean stubConfirm = true;
    boolean confirmDeleteResult = false;
    int confirmCalls = 0;

    @Override
    protected boolean confirmDelete() {
      confirmCalls++;
      if (stubConfirm) {
        return confirmDeleteResult;
      }
      return super.confirmDelete();
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
    when(services.getFileService()).thenReturn(mock(FileService.class));
    navigator = mock(Navigator.class);
    this.stage = stage;
    when(authService.isAdmin()).thenReturn(false);
    when(authService.isModerator()).thenReturn(true);
    when(resourceService.getById(RESOURCE_ID)).thenReturn(resource());
    loadPage();
  }

  private void loadPage() {
    try {
      controller = new TestableDetailsController();
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ResourceDetails.fxml"));
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
  void moderatorSeesEditAndDeleteButtons() {
    assertTrue(robot.lookup("#editButton").queryAs(Button.class).isVisible());
    assertTrue(robot.lookup("#deleteButton").queryAs(Button.class).isVisible());
  }

  @Test
  void plainUserSeesNeitherEditNorDeleteButtons() {
    robot.interact(
        () -> {
          when(authService.isAdmin()).thenReturn(false);
          when(authService.isModerator()).thenReturn(false);
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
  void moderatorDeleteClickOpensConfirmationModal() {
    robot.interact(
        () -> {
          controller.stubConfirm = false;
          controller.setResourceId(RESOURCE_ID);
        });

    Thread clicker = clickDeleteNoWait();
    DialogPane dialog = waitForDialog(robot);

    assertTrue(dialog.getScene().getWindow().isShowing(), "модалка удаления не открылась");
    assertEquals("Удалить ресурс?", dialog.getHeaderText());
    assertEquals("Ресурс будет удалён без возможности восстановления.", dialog.getContentText());

    pressDialogButton(robot, dialog, ButtonType.CANCEL);
    waitForClicker(clicker);

    assertModalClosed(robot);
    assertEquals(1, controller.confirmCalls);
    verify(resourceService, never()).delete(any());
    verify(navigator, never()).goTo(anyString());
  }

  @Test
  void confirmingModalClosesItDeletesAndNavigatesToList() {
    robot.interact(
        () -> {
          controller.stubConfirm = false;
          controller.setResourceId(RESOURCE_ID);
        });

    Thread clicker = clickDeleteNoWait();
    DialogPane dialog = waitForDialog(robot);
    pressDialogButton(robot, dialog, ButtonType.OK);
    waitForClicker(clicker);

    assertModalClosed(robot);
    verify(resourceService).delete(RESOURCE_ID);
    verify(navigator).goTo("/fxml/ResourceList.fxml");
  }

  private Resource resource() {
    return new Resource(
        RESOURCE_ID,
        "Годовой отчёт",
        "Описание ресурса",
        LocalDateTime.of(2024, 5, 6, 7, 8),
        LocalDateTime.of(2024, 5, 7, 9, 10),
        List.of());
  }

  /**
   * Нажимает кнопку удаления, не дожидаясь закрытия модального окна: настоящий {@code Alert}
   * блокирует поток JavaFX через {@code showAndWait}, поэтому клик уходит в отдельный поток, а тест
   * продолжает работать с диалогом.
   */
  private Thread clickDeleteNoWait() {
    Button delete = robot.lookup("#deleteButton").queryAs(Button.class);
    Thread clicker = new Thread(() -> robot.interact(delete::fire), "details-delete-click");
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

  private static void assertModalClosed(FxRobot robot) {
    assertTrue(robot.lookup(".dialog-pane").tryQuery().isEmpty(), "модалка удаления не закрылась");
  }
}
