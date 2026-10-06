package ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.DesktopApplicationClientJava.controllers.ResourceDetailsController;
import com.DesktopApplicationClientJava.entities.FileInfo;
import com.DesktopApplicationClientJava.entities.Resource;
import com.DesktopApplicationClientJava.navigation.Navigator;
import com.DesktopApplicationClientJava.services.FileService;
import com.DesktopApplicationClientJava.services.ResourceService;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.services.ServiceRegistry;
import com.DesktopApplicationClientJava.session.Session;
import com.DesktopApplicationClientJava.utils.ControllerWiring;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

@ExtendWith(ApplicationExtension.class)
class ResourceDetailsControllerTest {

  private static final UUID RESOURCE_ID = UUID.randomUUID();
  private static final UUID FILE_ID_1 = UUID.randomUUID();
  private static final UUID FILE_ID_2 = UUID.randomUUID();

  private ResourceService resourceService;
  private FileService fileService;
  private Navigator navigator;
  private TestableResourceDetailsController controller;

  static class TestableResourceDetailsController extends ResourceDetailsController {
    private File directory;

    void setDirectory(File directory) {
      this.directory = directory;
    }

    @Override
    protected File chooseDirectory(Window owner) {
      return directory;
    }
  }

  @Start
  void start(Stage stage) throws Exception {
    resourceService = mock(ResourceService.class);
    fileService = mock(FileService.class);
    ServiceRegistry services = mock(ServiceRegistry.class);
    when(services.getResourceService()).thenReturn(resourceService);
    when(services.getFileService()).thenReturn(fileService);
    navigator = mock(Navigator.class);

    controller = new TestableResourceDetailsController();
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ResourceDetails.fxml"));
    loader.setControllerFactory(clazz -> controller);
    ControllerWiring.wire(controller, navigator, new Session(), services);
    Parent root = loader.load();
    stage.setScene(new Scene(root));
    stage.show();
  }

  @Test
  void successfulLoadShowsMetadataAndFiles(FxRobot robot) {
    when(resourceService.getById(RESOURCE_ID)).thenReturn(resourceWithFiles());
    loadPage(robot);

    assertEquals("Годовой отчёт", textOf(robot, "#titleLabel"));
    assertEquals("Описание ресурса", textOf(robot, "#descriptionLabel"));
    assertEquals("06.05.2024 07:08", textOf(robot, "#createdLabel"));
    assertEquals("07.05.2024 09:10", textOf(robot, "#updatedLabel"));
    assertEquals(RESOURCE_ID.toString(), textOf(robot, "#uuidLabel"));

    TableView<FileInfo> table = filesTable(robot);
    assertEquals(2, table.getItems().size());
    assertEquals("report.pdf", table.getItems().get(0).getName());
    assertEquals("photo.png", table.getItems().get(1).getName());
    assertFalse(robot.lookup("#errorLabel").queryAs(Label.class).isVisible());
  }

  @Test
  void resourceNotFoundShowsErrorAndLeavesTableEmpty(FxRobot robot) {
    when(resourceService.getById(RESOURCE_ID)).thenThrow(new ServiceException("Ресурс не найден"));
    loadPage(robot);

    Label error = robot.lookup("#errorLabel").queryAs(Label.class);
    assertTrue(error.isVisible());
    assertEquals("Ресурс не найден", error.getText());
    assertTrue(filesTable(robot).getItems().isEmpty());
    verifyNoInteractions(navigator);
  }

  @Test
  void downloadSavesFileAndShowsStatus(FxRobot robot, @TempDir Path tempDir) {
    when(resourceService.getById(RESOURCE_ID)).thenReturn(resourceWithFiles());
    loadPage(robot);

    controller.setDirectory(tempDir.toFile());
    Path saved = tempDir.resolve("report.pdf");
    when(fileService.downloadFile(eq(RESOURCE_ID), eq(FILE_ID_1), any())).thenReturn(saved);

    robot.clickOn("Скачать");

    verify(fileService).downloadFile(RESOURCE_ID, FILE_ID_1, tempDir);
    assertEquals("Сохранено: report.pdf", textOf(robot, "#statusLabel"));
  }

  @Test
  void downloadErrorShowsInErrorLabel(FxRobot robot, @TempDir Path tempDir) {
    when(resourceService.getById(RESOURCE_ID)).thenReturn(resourceWithFiles());
    loadPage(robot);

    controller.setDirectory(tempDir.toFile());
    when(fileService.downloadFile(eq(RESOURCE_ID), eq(FILE_ID_1), any()))
        .thenThrow(new ServiceException("Файл недоступен"));

    robot.clickOn("Скачать");

    Label error = robot.lookup("#errorLabel").queryAs(Label.class);
    assertTrue(error.isVisible());
    assertEquals("Файл недоступен", error.getText());
  }

  @Test
  void cancelledDirectoryChooserDoesNotCallDownload(FxRobot robot) {
    when(resourceService.getById(RESOURCE_ID)).thenReturn(resourceWithFiles());
    loadPage(robot);

    controller.setDirectory(null);
    robot.clickOn("Скачать");

    verifyNoInteractions(fileService);
  }

  @Test
  void backNavigatesToResourceList(FxRobot robot) {
    robot.clickOn("#backButton");

    verify(navigator).goTo("/fxml/ResourceList.fxml");
  }

  private void loadPage(FxRobot robot) {
    robot.interact(
        () -> {
          controller.setResourceId(RESOURCE_ID);
          controller.load();
        });
  }

  private Resource resourceWithFiles() {
    FileInfo first = new FileInfo(FILE_ID_1, "report.pdf", "application/pdf", 12345L);
    FileInfo second = new FileInfo(FILE_ID_2, "photo.png", "image/png", 678L);
    return new Resource(
        RESOURCE_ID,
        "Годовой отчёт",
        "Описание ресурса",
        LocalDateTime.of(2024, 5, 6, 7, 8),
        LocalDateTime.of(2024, 5, 7, 9, 10),
        List.of(first, second));
  }

  private static String textOf(FxRobot robot, String query) {
    return robot.lookup(query).queryAs(Label.class).getText();
  }

  @SuppressWarnings("unchecked")
  private static TableView<FileInfo> filesTable(FxRobot robot) {
    return robot.lookup("#filesTable").queryAs(TableView.class);
  }
}
