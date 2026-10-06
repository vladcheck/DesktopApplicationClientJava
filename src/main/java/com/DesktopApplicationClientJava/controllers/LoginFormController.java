package com.DesktopApplicationClientJava.controllers;

import com.DesktopApplicationClientJava.controllers.utils.Controller;
import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.validation.EmailValidator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginFormController extends Controller {

  @FXML private TextField emailField;
  @FXML private PasswordField passwordField;
  @FXML private Label errorLabel;
  @FXML private Button submitButton;

  @FXML
  private void onSubmit(ActionEvent event) {
    String email = emailField.getText() == null ? "" : emailField.getText().trim();
    String password = passwordField.getText() == null ? "" : passwordField.getText();

    if (email.isBlank() || password.isBlank()) {
      showError("Заполните все поля");
      return;
    }

    String emailError = EmailValidator.validate(email);
    if (emailError != null) {
      showError(emailError);
      return;
    }

    try {
      // ← ВЫЗОВ СЕРВИСА (middleware): session уже обновлён сервисом при успехе
      User user = services.getAuthService().login(email, password);
      if (user == null) {
        return;
      }
      hideError();
      navigator.goTo("/fxml/ResourceList.fxml");
    } catch (ServiceException e) {
      showError(e.getMessage());
    }
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
