package com.DesktopApplicationClientJava.controllers;

import com.DesktopApplicationClientJava.controllers.utils.Controller;
import com.DesktopApplicationClientJava.entities.Role;
import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.services.data.RegisterData;
import com.DesktopApplicationClientJava.validation.EmailValidator;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;


public class UserCreateFormController extends Controller {

    @FXML
    private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button submitButton;
    @FXML private ChoiceBox<String> rolesChoiceBox;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;

    @FXML
    public void initialize() {
        // Способ 1: через setItems
        rolesChoiceBox.setItems(FXCollections.observableArrayList(
                Role.USER.toString(),
                Role.MODER.toString(),
                Role.ADMIN.toString()
        ));
        // Установить значение по умолчанию (опционально)
        rolesChoiceBox.setValue(Role.USER.toString());

        System.out.println(rolesChoiceBox.getValue());

    }

    @FXML
    private void onSubmit(ActionEvent event) {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        String roleStirng = rolesChoiceBox.getValue() == null ? "" : rolesChoiceBox.getValue();
        String firstName = firstNameField.getText() == null ? "" : firstNameField.getText().trim();
        String lastName = lastNameField.getText() == null ? "" : lastNameField.getText().trim();

        if (email.isBlank() || password.isBlank() || roleStirng.isBlank() || firstName.isBlank() || lastName.isBlank()) {
            showError("Заполните все поля");
            return;
        }

        if (!(roleStirng.equals(Role.USER.toString()) || roleStirng.equals(Role.MODER.toString())
                || roleStirng.equals(Role.ADMIN.toString()))) {
            showError("Роль не определена");
            return;
        }



        Role role = Role.valueOf(roleStirng);

        String emailError = EmailValidator.validate(email);
        if (emailError != null) {
            showError(emailError);
            return;
        }

        try {
            RegisterData registerData = new RegisterData(email, password, role, firstName, lastName);
            User user = services.getUsersService().create(registerData);
            if (user == null) {
                return;
            }
            hideError();
            navigator.goTo("/fxml/UserList.fxml");
        } catch (ServiceException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    void onCancel(ActionEvent event) {
        navigator.goTo("/fxml/UserList.fxml");
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
