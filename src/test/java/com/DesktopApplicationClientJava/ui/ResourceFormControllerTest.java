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

import com.DesktopApplicationClientJava.controllers.ResourceFormController;
import com.DesktopApplicationClientJava.entities.Resource;
import com.DesktopApplicationClientJava.navigation.Navigator;
import com.DesktopApplicationClientJava.services.ResourceService;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.services.ServiceRegistry;
import com.DesktopApplicationClientJava.session.Session;
import com.DesktopApplicationClientJava.utils.ControllerWiring;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

@ExtendWith(ApplicationExtension.class)
class ResourceFormControllerTest {

  private static final UUID RESOURCE_ID = UUID.randomUUID();
  private static final String TITLE = "Годовой отчёт";
  private static final String DESCRIPTION = "Описание документа";

  private ResourceService resourceService;
  private ServiceRegistry services;
  private Navigator navigator;
  private ResourceFormController controller;
  private Stage stage;

  @Start
  void start(Stage stage) throws Exception {
    resourceService = mock(ResourceService.class);
    services = mock(ServiceRegistry.class);
    when(services.getResourceService()).thenReturn(resourceService);
    navigator = mock(Navigator.class);
    this.stage = stage;
    loadForm(null);
  }

  /** Loads the form as {@code initialize()} would see it (resourceId set before FXML load). */
  private void loadForm(UUID resourceId) throws Exception {
    controller = new ResourceFormController();
    controller.setResourceId(resourceId);
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ResourceForm.fxml"));
    loader.setControllerFactory(
        clazz -> {
          ControllerWiring.wire(controller, navigator, new Session(), services);
          return controller;
        });
    Parent root = loader.load();
    stage.setScene(new Scene(root));
    stage.show();
  }

  private void reloadForm(FxRobot robot, UUID resourceId) {
    robot.interact(
        () -> {
          try {
            loadForm(resourceId);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  void createModeShowsNewDocumentTitle(FxRobot robot) {
    assertEquals("Новый документ", textOf(robot, "#formTitleLabel"));
    assertFalse(robot.lookup("#errorLabel").queryAs(Label.class).isVisible());
  }

  @Test
  void blankTitleShowsErrorAndDoesNotSave(FxRobot robot) {
    robot.clickOn("#saveButton");

    assertError(robot, "Название обязательно для заполнения");
    verify(resourceService, never()).create(anyString(), any());
    verify(resourceService, never()).update(any(), anyString(), any());
    verify(navigator, never()).goTo(anyString());
  }

  @Test
  void shortTitleShowsErrorAndDoesNotSave(FxRobot robot) {
    robot.clickOn("#titleField").write("ab");
    robot.clickOn("#saveButton");

    assertError(robot, "Название не может быть короче 3 символов");
    verify(resourceService, never()).create(anyString(), any());
    verify(navigator, never()).goTo(anyString());
  }

  @Test
  void longTitleShowsErrorAndDoesNotSave(FxRobot robot) {
    robot.clickOn("#titleField").write("a".repeat(256));
    robot.clickOn("#saveButton");

    assertError(robot, "Название не может быть длиннее 255 символов");
    verify(resourceService, never()).create(anyString(), any());
    verify(resourceService, never()).update(any(), anyString(), any());
    verify(navigator, never()).goTo(anyString());
  }

  @Test
  void validInputCreatesAndNavigatesToList(FxRobot robot) {
    robot.clickOn("#titleField").write(TITLE);
    robot.clickOn("#descriptionArea").write(DESCRIPTION);
    robot.clickOn("#saveButton");

    verify(resourceService).create(TITLE, DESCRIPTION);
    verify(navigator).goTo("/fxml/ResourceList.fxml");
    assertFalse(robot.lookup("#errorLabel").queryAs(Label.class).isVisible());
  }

  @Test
  void blankDescriptionIsSavedAsNull(FxRobot robot) {
    robot.clickOn("#titleField").write(TITLE);
    robot.clickOn("#saveButton");

    verify(resourceService).create(TITLE, null);
    verify(navigator).goTo("/fxml/ResourceList.fxml");
  }

  @Test
  void serviceErrorShowsInErrorLabelAndDoesNotNavigate(FxRobot robot) {
    when(resourceService.create(anyString(), any()))
        .thenThrow(new ServiceException("Не удалось сохранить"));

    robot.clickOn("#titleField").write(TITLE);
    robot.clickOn("#saveButton");

    assertError(robot, "Не удалось сохранить");
    verify(navigator, never()).goTo(anyString());
  }

  @Test
  void cancelNavigatesToListWithoutSaving(FxRobot robot) {
    robot.clickOn("#titleField").write(TITLE);
    robot.clickOn("#cancelButton");

    verify(navigator).goTo("/fxml/ResourceList.fxml");
    verify(resourceService, never()).create(anyString(), any());
    verify(resourceService, never()).update(any(), anyString(), any());
  }

  @Test
  void editModePrefillsExistingValues(FxRobot robot) {
    when(resourceService.getById(RESOURCE_ID)).thenReturn(existingResource());
    reloadForm(robot, RESOURCE_ID);

    assertEquals("Редактирование документа", textOf(robot, "#formTitleLabel"));
    assertEquals("Старое название", fieldText(robot, "#titleField"));
    assertEquals("Старое описание", areaText(robot, "#descriptionArea"));
  }

  @Test
  void editModeSaveUpdatesAndNavigatesToList(FxRobot robot) {
    when(resourceService.getById(RESOURCE_ID)).thenReturn(existingResource());
    reloadForm(robot, RESOURCE_ID);

    robot.interact(
        () -> robot.lookup("#titleField").queryAs(TextField.class).setText("Новое название"));
    robot.clickOn("#saveButton");

    verify(resourceService).update(RESOURCE_ID, "Новое название", "Старое описание");
    verify(resourceService, never()).create(anyString(), any());
    verify(navigator).goTo("/fxml/ResourceList.fxml");
  }

  @Test
  void editModeLoadErrorShowsInErrorLabel(FxRobot robot) {
    when(resourceService.getById(RESOURCE_ID)).thenThrow(new ServiceException("Ресурс не найден"));
    reloadForm(robot, RESOURCE_ID);

    assertError(robot, "Ресурс не найден");
  }

  private Resource existingResource() {
    return new Resource(
        RESOURCE_ID,
        "Старое название",
        "Старое описание",
        LocalDateTime.of(2024, 5, 6, 7, 8),
        LocalDateTime.of(2024, 5, 7, 9, 10),
        List.of());
  }

  private static void assertError(FxRobot robot, String expected) {
    Label error = robot.lookup("#errorLabel").queryAs(Label.class);
    assertTrue(error.isVisible());
    assertEquals(expected, error.getText());
  }

  private static String textOf(FxRobot robot, String query) {
    return robot.lookup(query).queryAs(Label.class).getText();
  }

  private static String fieldText(FxRobot robot, String query) {
    return robot.lookup(query).queryAs(TextField.class).getText();
  }

  private static String areaText(FxRobot robot, String query) {
    return robot.lookup(query).queryAs(TextArea.class).getText();
  }
}
