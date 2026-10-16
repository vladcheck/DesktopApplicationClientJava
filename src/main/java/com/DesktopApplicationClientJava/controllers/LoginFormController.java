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

  @FXML TextField emailField;
  @FXML PasswordField passwordField;
  @FXML Label errorLabel;
  @FXML Button submitButton;

  @Override
  protected Label getErrorLabel() {
    return errorLabel;
  }

  @Override
  protected boolean interceptAuthFailure() {
    return false;
  }

  @FXML
  void onSubmit(ActionEvent event) {
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
      User user = services.getAuthService().login(email, password);
      if (user == null) {
        return;
      }
      hideError();
      navigator.goTo("/fxml/ResourceList.fxml");
    } catch (ServiceException e) {
      showError(e);
    }
  }
}
