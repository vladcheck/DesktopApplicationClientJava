package com.DesktopApplicationClientJava.controllers;

import com.DesktopApplicationClientJava.controllers.utils.Controller;
import com.DesktopApplicationClientJava.entities.Resource;
import com.DesktopApplicationClientJava.services.ServiceException;

import java.io.BufferedReader;
import java.util.UUID;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import lombok.Setter;

public class ResourceFormController extends Controller {

    private static final int TITLE_MIN = 3;
    private static final int TITLE_MAX = 255;

   @FXML private Label formTitleLabel;
   @FXML private TextField titleField;
   @FXML private TextArea descriptionArea;
   @FXML private Label errorLabel;
   @FXML private Button cancelButton;
   @FXML private Button saveButton;

   @Setter private UUID resourceId;

   @FXML
   private void initialize() {
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
           showError(e.getMessage());
       }
   }

   @FXML
   private void onSave(ActionEvent event) {
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
       }

       try {
           if (resourceId == null) {
               services.getResourceService().create(title, description);
           } else {
               services.getResourceService().update(resourceId, title, description);
           }
           navigator.goTo("/fxml/ResourceList.fxml");
       } catch (ServiceException e) {
            showError(e.getMessage());
       }
   }

   @FXML
   private void onCancel(ActionEvent event) {
       navigator.goTo("/fxml/ResourceList.fxml");
   }

   private void showError(String message) {
       errorLabel.setText(message);
       errorLabel.setVisible(true);
       errorLabel.setManaged(true);
   }

   private void hideError() {
       errorLabel.setText("");
       errorLabel.setVisible(false);
       errorLabel.setManaged(false);
   }

}