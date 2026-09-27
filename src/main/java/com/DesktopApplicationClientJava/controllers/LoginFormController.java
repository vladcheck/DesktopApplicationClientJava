package com.DesktopApplicationClientJava.controllers;

import com.DesktopApplicationClientJava.controllers.utils.Controller;
import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.utils.FieldError;
import com.DesktopApplicationClientJava.validation.EmailValidator;
import com.DesktopApplicationClientJava.validation.FirstNameValidator;
import com.DesktopApplicationClientJava.validation.LastNameValidator;
import com.DesktopApplicationClientJava.validation.PasswordValidator;
import com.DesktopApplicationClientJava.validation.utils.Validator;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginFormController extends Controller {
    @FXML
    private TextField firstNameField;
    @FXML
    private Label firstNameErrorLabel;
    private FieldError firstNameError;

    @FXML
    private TextField lastNameField;
    @FXML
    private Label lastNameErrorLabel;
    private FieldError lastNameError;

    @FXML
    private TextField emailField;
    @FXML
    private Label emailErrorLabel;
    private FieldError emailError;

    @FXML
    private PasswordField passwordField;
    @FXML
    private Label passwordErrorLabel;
    private FieldError passwordError;

    @FXML
    private Button submitButton;

    @FXML
    private void initialize() {
        firstNameError = new FieldError(firstNameErrorLabel);
        lastNameError = new FieldError(lastNameErrorLabel);
        emailError = new FieldError(emailErrorLabel);
        passwordError = new FieldError(passwordErrorLabel);

        emailField.textProperty().addListener((o, a, b) -> emailError.hide());
        passwordField.textProperty().addListener((o, a, b) -> passwordError.hide());
    }

    @FXML
    private void onSubmit(ActionEvent event) {
        if (!isValid())
            return;

        User user = authenticate(firstNameField.getText(), lastNameField.getText(), emailField.getText());
        if (user == null)
            return;

        session.setCurrentUser(user);
        navigator.goTo("/fxml/Search.fxml");
    }

    private boolean isValid() {
        return isFirstNameValid() & isLastNameValid() & isEmailValid() & isPasswordValid();
    }

    private boolean isFirstNameValid() {
        return isFieldValid(firstNameField, new FirstNameValidator(), firstNameError);
    }

    private boolean isLastNameValid() {
        return isFieldValid(lastNameField, new LastNameValidator(), lastNameError);
    }

    private boolean isEmailValid() {
        return isFieldValid(emailField, new EmailValidator(), emailError);
    }

    private boolean isPasswordValid() {
        return isFieldValid(passwordField, new PasswordValidator(), passwordError);
    }

    private boolean isFieldValid(TextField field, Validator validator, FieldError errorLabel) {
        String input = field.getText();
        var error = validator.isValid(input);
        if (error == null) {
            errorLabel.hide();
            return true;
        }
        errorLabel.show(error);
        return false;
    }

    private User authenticate(String firstName, String lastName, String email) {
        return new User(firstName, lastName, email);
    }
}