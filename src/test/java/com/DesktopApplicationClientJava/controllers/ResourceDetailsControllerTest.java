package com.DesktopApplicationClientJava.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Epic("Ресурсы")
@Feature("№3 Просмотр метаданных ресурса")
@DisplayName("Детали ресурса")
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
    boolean confirmDeleteResult = true;
    boolean stubConfirm = true;
    int confirmCalls = 0;

    void setDirectory(File directory) {
      this.directory = directory;
    }

    @Override
    protected File chooseDirectory(Window owner) {
      return directory;
    }

    @Override
    protected boolean confirmDelete() {
      confirmCalls++;
      if (stubConfirm) {
        return confirmDeleteResult;
      }
      return super.confirmDelete();
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
  @Tag("role:user")
  @DisplayName("Загрузка показывает метаданные и файлы")
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
  @Tag("role:user")
  @DisplayName("Отсутствующий ресурс: ошибка и пустая таблица")
  void resourceNotFoundShowsErrorAndLeavesTableEmpty() {
    when(resourceService.getById(RESOURCE_ID)).thenThrow(new ServiceException("Ресурс не найден"));
    controller.setResourceId(RESOURCE_ID);

    assertTrue(controller.errorLabel.isVisible());
    assertEquals("Ресурс не найден", controller.errorLabel.getText());
    assertTrue(controller.filesTable.getItems().isEmpty());
    verifyNoInteractions(navigator);
  }

  @Test
  @Story("Скачивание")
  @Tag("role:user")
  @DisplayName("Скачивание сохраняет файл и показывает статус")
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
  @Story("Скачивание")
  @Tag("role:user")
  @DisplayName("Ошибка скачивания показывается в строке ошибки")
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
  @Story("Скачивание")
  @Tag("role:user")
  @DisplayName("Отмена выбора каталога не вызывает скачивание")
  void cancelledDirectoryChooserDoesNotCallDownload() {
    when(resourceService.getById(RESOURCE_ID)).thenReturn(resourceWithFiles());
    controller.setResourceId(RESOURCE_ID);

    FileInfo toDownload = controller.filesTable.getItems().get(0);
    controller.setDirectory(null);

    controller.onDownload(toDownload);

    verifyNoInteractions(fileService);
  }

  @Test
  @Tag("role:user")
  @DisplayName("Кнопка «Назад» ведёт в список ресурсов")
  void backNavigatesToResourceList() {
    controller.onBack(new ActionEvent());
    verify(navigator).goTo("/fxml/ResourceList.fxml");
  }

  @Test
  @Story("Удаление с подтверждением")
  @Tag("role:moder")
  @DisplayName("Подтверждённое удаление вызывает сервис и ведёт в список")
  void confirmedDeleteCallsServiceAndNavigatesToList() {
    when(resourceService.getById(RESOURCE_ID)).thenReturn(resourceWithFiles());
    controller.setResourceId(RESOURCE_ID);
    controller.confirmDeleteResult = true;

    controller.onDelete(new ActionEvent());

    verify(resourceService).delete(RESOURCE_ID);
    verify(navigator).goTo("/fxml/ResourceList.fxml");
    assertFalse(controller.errorLabel.isVisible());
  }

  @Test
  @Story("Удаление с подтверждением")
  @Tag("role:moder")
  @DisplayName("Отменённое удаление не вызывает сервис")
  void cancelledDeleteDoesNotCallServiceNorNavigate() {
    when(resourceService.getById(RESOURCE_ID)).thenReturn(resourceWithFiles());
    controller.setResourceId(RESOURCE_ID);
    controller.confirmDeleteResult = false;

    controller.onDelete(new ActionEvent());

    assertEquals(1, controller.confirmCalls);
    verify(resourceService, never()).delete(any());
    verifyNoInteractions(navigator);
  }

  @Test
  @Story("Удаление с подтверждением")
  @Tag("role:moder")
  @DisplayName("Ошибка удаления показывается без перехода")
  void deleteErrorShowsInErrorLabelWithoutNavigation() {
    when(resourceService.getById(RESOURCE_ID)).thenReturn(resourceWithFiles());
    controller.setResourceId(RESOURCE_ID);
    controller.confirmDeleteResult = true;
    org.mockito.Mockito.doThrow(new ServiceException("Удаление запрещено"))
        .when(resourceService)
        .delete(RESOURCE_ID);

    controller.onDelete(new ActionEvent());

    assertTrue(controller.errorLabel.isVisible());
    assertEquals("Удаление запрещено", controller.errorLabel.getText());
    verify(navigator, never()).goTo(any());
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
