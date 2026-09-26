package com.DesktopApplicationClientJava.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginFormController {
    private static final int MIN_PASSWORD_LENGTH = 8;

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
        emailError = new FieldError(emailErrorLabel);
        passwordError = new FieldError(passwordErrorLabel);

        emailField.textProperty().addListener((o, a, b) -> emailError.hide());
        passwordField.textProperty().addListener((o, a, b) -> passwordError.hide());
    }

    @FXML
    private void onSubmit(ActionEvent event) {
        if (!isValid()) {
            return;
        }
    }

    private boolean isValid() {
        return isEmailValid() & isPasswordValid();
    }

    private boolean isEmailValid() {
        String email = emailField.getText();

        if (email.isBlank()) {
            emailError.show("Это поле обязательно для заполнения");
            return false;
        }
        if (email.length() < 5) {
            emailError.show("Почта не может быть короче 5 символов");
            return false;
        }
        if (email.length() > 100) {
            emailError.show("Почта не должна быть длиннее 100 символов");
            return false;
        }
        if (!email.contains("@")) {
            emailError.show("Неверный формат почты");
            return false;
        }

        emailError.hide();
        return true;
    }

    private boolean isPasswordValid() {
        String pw = passwordField.getText();

        if (pw.isBlank()) {
            passwordError.show("Это поле обязательно для заполнения");
            return false;
        }
        if (pw.length() < MIN_PASSWORD_LENGTH) {
            passwordError.show("Пароль не может быть короче " + MIN_PASSWORD_LENGTH + " символов");
            return false;
        }

        passwordError.hide();
        return true;
    }
}