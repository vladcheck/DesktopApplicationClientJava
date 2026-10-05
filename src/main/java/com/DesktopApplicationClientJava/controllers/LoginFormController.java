package com.DesktopApplicationClientJava.controllers;

import com.DesktopApplicationClientJava.controllers.utils.Controller;
import com.DesktopApplicationClientJava.entities.Role;
import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.utils.FieldError;
import com.DesktopApplicationClientJava.validation.EmailValidator;
import com.DesktopApplicationClientJava.validation.FirstNameValidator;
import com.DesktopApplicationClientJava.validation.LastNameValidator;
import com.DesktopApplicationClientJava.validation.PasswordValidator;
import com.DesktopApplicationClientJava.validation.ThirdNameValidator;
import com.DesktopApplicationClientJava.validation.utils.FieldErrorDecoration;
import java.util.UUID;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import net.synedra.validatorfx.Validator;

public class LoginFormController extends Controller {
  private final Validator validator = new Validator();

  @FXML private TextField firstNameField;
  @FXML private Label firstNameErrorLabel;
  private FieldError firstNameError;

  @FXML private TextField lastNameField;
  @FXML private Label lastNameErrorLabel;
  private FieldError lastNameError;

  @FXML private TextField thirdNameField;
  @FXML private Label thirdNameErrorLabel;
  private FieldError thirdNameError;

  @FXML private TextField emailField;
  @FXML private Label emailErrorLabel;
  private FieldError emailError;

  @FXML private PasswordField passwordField;
  @FXML private Label passwordErrorLabel;
  private FieldError passwordError;

  @FXML private Button submitButton;

  @FXML
  private void initialize() {
    firstNameError = new FieldError(firstNameErrorLabel);
    lastNameError = new FieldError(lastNameErrorLabel);
    thirdNameError = new FieldError(thirdNameErrorLabel);
    emailError = new FieldError(emailErrorLabel);
    passwordError = new FieldError(passwordErrorLabel);

    setupValidation();
  }

  private void setupValidation() {
    FirstNameValidator.createCheck(validator, firstNameField.textProperty())
        .decoratingWith(FieldErrorDecoration.forError(firstNameError))
        .decorates(firstNameField)
        .immediateClear();
    LastNameValidator.createCheck(validator, lastNameField.textProperty())
        .decoratingWith(FieldErrorDecoration.forError(lastNameError))
        .decorates(lastNameField)
        .immediateClear();
    ThirdNameValidator.createCheck(validator, thirdNameField.textProperty())
        .decoratingWith(FieldErrorDecoration.forError(thirdNameError))
        .decorates(thirdNameField)
        .immediateClear();
    EmailValidator.createCheck(validator, emailField.textProperty())
        .decoratingWith(FieldErrorDecoration.forError(emailError))
        .decorates(emailField)
        .immediateClear();
    PasswordValidator.createCheck(validator, passwordField.textProperty())
        .decoratingWith(FieldErrorDecoration.forError(passwordError))
        .decorates(passwordField)
        .immediateClear();
  }

  @FXML
  private void onSubmit(ActionEvent event) {
    if (!validator.validate()) return;

    User user =
        authenticate(firstNameField.getText(), lastNameField.getText(), emailField.getText());
    if (user == null) return;

    session.setCurrentUser(user);
    navigator.goTo("/fxml/Search.fxml");
  }

  private User authenticate(String firstName, String lastName, String email) {
    return new User(UUID.randomUUID(), email, Role.USER, firstName, lastName, true);
  }
}
