package com.DesktopApplicationClientJava.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
    resourceService = mock(ResourceService.class);
    fileService = mock(FileService.class);
    navigator = mock(Navigator.class);

    ServiceRegistry services = mock(ServiceRegistry.class);
    when(services.getResourceService()).thenReturn(resourceService);
    when(services.getFileService()).thenReturn(fileService);

    controller = new TestableResourceDetailsController();
    ControllerWiring.wire(controller, navigator, new Session(), services);

    controller.titleLabel = new Label();
    controller.descriptionLabel = new Label();
    controller.createdLabel = new Label();
    controller.updatedLabel = new Label();
    controller.uuidLabel = new Label();
    controller.errorLabel = new Label();
    controller.statusLabel = new Label();
    controller.filesTable = new TableView<>();

    // onDownload does backButton.getScene().getWindow() before calling chooseDirectory.
    // Attaching the button to a Scene keeps that expression from NPE-ing; the window itself
    // is never dereferenced because our chooseDirectory override ignores its argument.
    controller.backButton = new Button();
    new Scene(new StackPane(controller.backButton));
  }

  @Test
  void successfulLoadShowsMetadataAndFiles() {
    when(resourceService.getById(RESOURCE_ID)).thenReturn(resourceWithFiles());
    controller.setResourceId(RESOURCE_ID);

    assertEquals("Годовой отчёт", controller.titleLabel.getText());
    assertEquals("Описание ресурса", controller.descriptionLabel.getText());
    assertEquals("06.05.2024 07:08", controller.createdLabel.getText());
    assertEquals("07.05.2024 09:10", controller.updatedLabel.getText());
    assertEquals(RESOURCE_ID.toString(), controller.uuidLabel.getText());
    assertEquals(2, controller.filesTable.getItems().size());
    assertEquals("report.pdf", controller.filesTable.getItems().get(0).getName());
    assertFalse(controller.errorLabel.isVisible());
  }

  @Test
  void resourceNotFoundShowsErrorAndLeavesTableEmpty() {
    when(resourceService.getById(RESOURCE_ID)).thenThrow(new ServiceException("Ресурс не найден"));
    controller.setResourceId(RESOURCE_ID);

    assertTrue(controller.errorLabel.isVisible());
    assertEquals("Ресурс не найден", controller.errorLabel.getText());
    assertTrue(controller.filesTable.getItems().isEmpty());
    verifyNoInteractions(navigator);
  }

  @Test
  void downloadSavesFileAndShowsStatus(@TempDir Path tempDir) {
    when(resourceService.getById(RESOURCE_ID)).thenReturn(resourceWithFiles());
    controller.setResourceId(RESOURCE_ID);

    FileInfo toDownload = controller.filesTable.getItems().get(0);
    controller.setDirectory(tempDir.toFile());
    Path saved = tempDir.resolve("report.pdf");
    when(fileService.downloadFile(eq(RESOURCE_ID), eq(FILE_ID_1), any())).thenReturn(saved);

    controller.onDownload(toDownload);

    verify(fileService).downloadFile(RESOURCE_ID, FILE_ID_1, tempDir);
    assertEquals("Сохранено: report.pdf", controller.statusLabel.getText());
  }

  @Test
  void downloadErrorShowsInErrorLabel(@TempDir Path tempDir) {
    when(resourceService.getById(RESOURCE_ID)).thenReturn(resourceWithFiles());
    controller.setResourceId(RESOURCE_ID);

    FileInfo toDownload = controller.filesTable.getItems().get(0);
    controller.setDirectory(tempDir.toFile());
    when(fileService.downloadFile(eq(RESOURCE_ID), eq(FILE_ID_1), any()))
        .thenThrow(new ServiceException("Файл недоступен"));

    controller.onDownload(toDownload);

    assertTrue(controller.errorLabel.isVisible());
    assertEquals("Файл недоступен", controller.errorLabel.getText());
  }

  @Test
  void cancelledDirectoryChooserDoesNotCallDownload() {
    when(resourceService.getById(RESOURCE_ID)).thenReturn(resourceWithFiles());
    controller.setResourceId(RESOURCE_ID);

    FileInfo toDownload = controller.filesTable.getItems().get(0);
    controller.setDirectory(null);

    controller.onDownload(toDownload);

    verifyNoInteractions(fileService);
  }

  @Test
  void backNavigatesToResourceList() {
    controller.onBack(new ActionEvent());
    verify(navigator).goTo("/fxml/ResourceList.fxml");
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
}
