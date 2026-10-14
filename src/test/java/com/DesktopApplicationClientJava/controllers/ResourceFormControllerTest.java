package com.DesktopApplicationClientJava.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResourceFormControllerTest {

  private static final UUID RESOURCE_ID = UUID.randomUUID();
  private static final String TITLE = "Годовой отчёт";
  private static final String DESCRIPTION = "Описание документа";

  private ResourceService resourceService;
  private Navigator navigator;
  private ResourceFormController controller;

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
    navigator = mock(Navigator.class);

    ServiceRegistry services = mock(ServiceRegistry.class);
    when(services.getResourceService()).thenReturn(resourceService);

    controller = new ResourceFormController();
    ControllerWiring.wire(controller, navigator, new Session(), services);

    controller.titleField = new TextField();
    controller.descriptionArea = new TextArea();
    controller.errorLabel = new Label();
    controller.formTitleLabel = new Label();

    controller.initialize();
  }

  @Test
  void createModeShowsNewDocumentTitle() {
    assertEquals("Новый документ", controller.formTitleLabel.getText());
    assertFalse(controller.errorLabel.isVisible());
  }

  @Test
  void blankTitleShowsErrorAndDoesNotSave() {
    controller.onSave(new ActionEvent());

    assertTrue(controller.errorLabel.isVisible());
    assertEquals("Название обязательно для заполнения", controller.errorLabel.getText());
    verify(resourceService, never()).create(anyString(), any());
    verify(navigator, never()).goTo(anyString());
  }

  @Test
  void shortTitleShowsErrorAndDoesNotSave() {
    controller.titleField.setText("ab");
    controller.onSave(new ActionEvent());

    assertTrue(controller.errorLabel.isVisible());
    assertEquals("Название не может быть короче 3 символов", controller.errorLabel.getText());
    verify(resourceService, never()).create(anyString(), any());
  }

  @Test
  void longTitleShowsErrorAndDoesNotSave() {
    controller.titleField.setText("a".repeat(256));
    controller.onSave(new ActionEvent());

    assertTrue(controller.errorLabel.isVisible());
    assertEquals("Название не может быть длиннее 255 символов", controller.errorLabel.getText());
    verify(resourceService, never()).create(anyString(), any());
    verify(navigator, never()).goTo(anyString());
  }

  @Test
  void validInputCreatesAndNavigatesToList() {
    controller.titleField.setText(TITLE);
    controller.descriptionArea.setText(DESCRIPTION);
    controller.onSave(new ActionEvent());

    verify(resourceService).create(TITLE, DESCRIPTION);
    verify(navigator).goTo("/fxml/ResourceList.fxml");
    assertFalse(controller.errorLabel.isVisible());
  }

  @Test
  void blankDescriptionIsSavedAsNull() {
    controller.titleField.setText(TITLE);
    controller.onSave(new ActionEvent());

    verify(resourceService).create(TITLE, null);
    verify(navigator).goTo("/fxml/ResourceList.fxml");
  }

  @Test
  void serviceErrorShowsInErrorLabelAndDoesNotNavigate() {
    when(resourceService.create(anyString(), any()))
        .thenThrow(new ServiceException("Не удалось сохранить"));

    controller.titleField.setText(TITLE);
    controller.onSave(new ActionEvent());

    assertTrue(controller.errorLabel.isVisible());
    assertEquals("Не удалось сохранить", controller.errorLabel.getText());
    verify(navigator, never()).goTo(anyString());
  }

  @Test
  void cancelNavigatesToListWithoutSaving() {
    controller.titleField.setText(TITLE);
    controller.onCancel(new ActionEvent());

    verify(navigator).goTo("/fxml/ResourceList.fxml");
    verify(resourceService, never()).create(anyString(), any());
    verify(resourceService, never()).update(any(), anyString(), any());
  }

  @Test
  void editModePrefillsExistingValues() {
    when(resourceService.getById(RESOURCE_ID)).thenReturn(existingResource());
    controller.setResourceId(RESOURCE_ID);

    assertEquals("Редактирование документа", controller.formTitleLabel.getText());
    assertEquals("Старое название", controller.titleField.getText());
    assertEquals("Старое описание", controller.descriptionArea.getText());
  }

  @Test
  void editModeSaveUpdatesAndNavigatesToList() {
    when(resourceService.getById(RESOURCE_ID)).thenReturn(existingResource());
    controller.setResourceId(RESOURCE_ID);

    controller.titleField.setText("Новое название");
    controller.onSave(new ActionEvent());

    verify(resourceService).update(RESOURCE_ID, "Новое название", "Старое описание");
    verify(resourceService, never()).create(anyString(), any());
    verify(navigator).goTo("/fxml/ResourceList.fxml");
  }

  @Test
  void editModeLoadErrorShowsInErrorLabel() {
    when(resourceService.getById(RESOURCE_ID)).thenThrow(new ServiceException("Ресурс не найден"));
    controller.setResourceId(RESOURCE_ID);

    assertTrue(controller.errorLabel.isVisible());
    assertEquals("Ресурс не найден", controller.errorLabel.getText());
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
}
