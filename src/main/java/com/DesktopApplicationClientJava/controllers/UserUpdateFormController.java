package com.DesktopApplicationClientJava.controllers;

import com.DesktopApplicationClientJava.controllers.utils.Controller;
import com.DesktopApplicationClientJava.entities.Role;
import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.services.data.UpdateUserData;
import java.util.UUID;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class UserUpdateFormController extends Controller {

    @FXML private Label subtitleLabel;
    @FXML private TextField emailField;
    @FXML private ChoiceBox<Role> roleChoiceBox;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private CheckBox enabledCheckBox;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private UUID userId;
    private User current;

    // ===== Lifecycle =====

    @FXML
    private void initialize() {
        hideError();
        roleChoiceBox.setItems(FXCollections.observableArrayList(Role.values()));
    }

    /** Вызывается навигатором при переходе на форму. */
    public void setUserId(UUID userId) {
        this.userId = userId;
        loadUser();
    }

    // ===== Loading =====

    private void loadUser() {
        if (userId == null || services == null || services.getUsersService() == null) {
            return;
        }
        hideError();
        setFormDisabled(true);

        var task =
                new Task<User>() {
                    @Override
                    protected User call() {
                        return services.getUsersService().getById(userId);
                    }
                };

        task.setOnSucceeded(
                e -> {
                    current = task.getValue();
                    if (current == null) {
                        showError("Пользователь не найден");
                        setFormDisabled(true);
                        return;
                    }
                    populateForm(current);
                    setFormDisabled(false);
                });

        task.setOnFailed(
                e -> {
                    var ex = task.getException();
                    String msg =
                            ex instanceof ServiceException se ? se.getMessage() : "Не удалось загрузить пользователя";
                    showError(msg);
                    setFormDisabled(true);
                });

        startDaemon(task, "user-update-load");
    }

    private void populateForm(User user) {
        emailField.setText(user.getEmail() == null ? "" : user.getEmail());
        firstNameField.setText(user.getFirstName() == null ? "" : user.getFirstName());
        lastNameField.setText(user.getLastName() == null ? "" : user.getLastName());
        roleChoiceBox.getSelectionModel().select(user.getRole());
        enabledCheckBox.setSelected(Boolean.TRUE.equals(user.getEnabled()));
        subtitleLabel.setText(user.getEmail() == null ? "" : user.getEmail());
    }

    // ===== Event handlers =====

    @FXML
    private void onSave(ActionEvent e) {
        if (current == null) {
            return;
        }
        var data = collectAndValidate();
        if (data == null) {
            return;
        }

        hideError();
        setFormDisabled(true);

        var task =
                new Task<User>() {
                    @Override
                    protected User call() {
                        return services.getUsersService().update(current.getUuid(), data);
                    }
                };

        task.setOnSucceeded(
                ev -> {
                    setFormDisabled(false);
                    navigator.goTo("/fxml/UserList.fxml");
                });

        task.setOnFailed(
                ev -> {
                    setFormDisabled(false);
                    var ex = task.getException();
                    String msg =
                            ex instanceof ServiceException se ? se.getMessage() : "Не удалось сохранить изменения";
                    showError(msg);
                });

        startDaemon(task, "user-update-save");
    }

    @FXML
    private void onCancel(ActionEvent e) {
        navigator.goTo("/fxml/UserList.fxml");
    }

    // ===== Validation / data =====

    /**
     * Собирает данные формы. Если валидация не прошла — показывает ошибку
     * и возвращает {@code null}.
     */
    private UpdateUserData collectAndValidate() {
        String email = safeTrim(emailField.getText());
        String firstName = safeTrim(firstNameField.getText());
        String lastName = safeTrim(lastNameField.getText());
        Role role = roleChoiceBox.getValue();
        boolean enabled = enabledCheckBox.isSelected();

        if (email == null || email.isBlank()) {
            showError("Укажите почту");
            return null;
        }
        if (!email.contains("@")) {
            showError("Почта указана некорректно");
            return null;
        }
        if (firstName == null || firstName.isBlank()) {
            showError("Укажите имя");
            return null;
        }
        if (lastName == null || lastName.isBlank()) {
            showError("Укажите фамилию");
            return null;
        }
        if (role == null) {
            showError("Выберите роль");
            return null;
        }

        // NB: UpdateUserData не содержит пароль — смена пароля выполняется отдельно.
        // Поле passwordField оставлено в форме для будущей поддержки, но сейчас
        // в UpdateUserData не передаётся.
        return new UpdateUserData(email, role, firstName, lastName, enabled);
    }

    // ===== UI helpers =====

    private void setFormDisabled(boolean disabled) {
        emailField.setDisable(disabled);
        roleChoiceBox.setDisable(disabled);
        firstNameField.setDisable(disabled);
        lastNameField.setDisable(disabled);
        enabledCheckBox.setDisable(disabled);
        saveButton.setDisable(disabled);
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

    private static String safeTrim(String value) {
        return value == null ? null : value.trim();
    }

    private static void startDaemon(Task<?> task, String name) {
        var thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
    }
}