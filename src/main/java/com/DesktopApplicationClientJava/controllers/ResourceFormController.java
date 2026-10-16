package com.DesktopApplicationClientJava.controllers;

import com.DesktopApplicationClientJava.controllers.utils.Controller;
import com.DesktopApplicationClientJava.entities.Resource;
import com.DesktopApplicationClientJava.services.ServiceException;
import java.util.UUID;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ResourceFormController extends Controller {

  private static final int TITLE_MIN = 3;
  private static final int TITLE_MAX = 255;

  @FXML Label formTitleLabel;
  @FXML TextField titleField;
  @FXML TextArea descriptionArea;
  @FXML Label errorLabel;
  @FXML Button cancelButton;
  @FXML Button saveButton;

  @Override
  protected Label getErrorLabel() {
    return errorLabel;
  }

  private UUID resourceId;

  /**
   * Navigator sets the id <i>after</i> FXML load (see {@code Navigator.goTo}), so the setter itself
   * must switch the form into edit mode when the view is already initialized. Setting the id before
   * load is also honored by {@link #initialize()}.
   */
  public void setResourceId(UUID resourceId) {
    this.resourceId = resourceId;
    if (formTitleLabel != null && resourceId != null && services != null) {
      formTitleLabel.setText("Редактирование документа");
      loadExisting();
    }
  }

  @FXML
  void initialize() {
    hideError();
    if (resourceId == null) {
      formTitleLabel.setText("Новый документ");
    } else {
      formTitleLabel.setText("Редактирование документа");
      loadExisting();
    }
  }

  private void loadExisting() {
    try {
      Resource r = services.getResourceService().getById(resourceId);
      titleField.setText(r.getTitle() == null ? "" : r.getTitle());
      descriptionArea.setText(r.getDescription() == null ? "" : r.getDescription());
    } catch (ServiceException e) {
      showError(e);
    }
  }

  @FXML
  void onSave(ActionEvent event) {
    hideError();
    String title = titleField.getText() == null ? "" : titleField.getText().trim();
    String description = descriptionArea.getText() == null ? "" : descriptionArea.getText().trim();
    String descriptionOrNull = description.isBlank() ? null : description;

    if (title.isBlank()) {
      showError("Название обязательно для заполнения");
      titleField.requestFocus();
      return;
    }
    if (title.length() < TITLE_MIN) {
      showError("Название не может быть короче " + TITLE_MIN + " символов");
      titleField.requestFocus();
      return;
    }
    if (title.length() > TITLE_MAX) {
      showError("Название не может быть длиннее " + TITLE_MAX + " символов");
      titleField.requestFocus();
      return;
    }

    try {
      if (resourceId == null) {
        services.getResourceService().create(title, descriptionOrNull);
      } else {
        services.getResourceService().update(resourceId, title, descriptionOrNull);
      }
      navigator.goTo("/fxml/ResourceList.fxml");
    } catch (ServiceException e) {
      showError(e);
    }
  }

  @FXML
  void onCancel(ActionEvent event) {
    navigator.goTo("/fxml/ResourceList.fxml");
  }
}
