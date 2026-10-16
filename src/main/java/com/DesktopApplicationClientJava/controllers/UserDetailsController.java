package com.DesktopApplicationClientJava.controllers;

import com.DesktopApplicationClientJava.controllers.utils.Controller;
import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.services.ServiceException;
import java.util.UUID;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;

public class UserDetailsController extends Controller {

  @FXML private Label fullNameLabel;
  @FXML private Label emailLabel;
  @FXML private Label roleLabel;
  @FXML private Label enabledLabel;
  @FXML private Label uuidLabel;
  @FXML private Label errorLabel;
  @FXML private Label statusLabel;
  @FXML private Button backButton;
  @FXML private Button editButton;
  @FXML private Button deleteButton;

  private UUID userId;
  private User current;

  // ===== Lifecycle =====

  @FXML
  private void initialize() {
    hideError();

    // Кнопки редактирования и удаления доступны только администраторам.
    var authService = services == null ? null : services.getAuthService();
    boolean isAdmin = authService != null && authService.isAdmin();

    editButton.setVisible(isAdmin);
    editButton.setManaged(isAdmin);

    deleteButton.setVisible(isAdmin);
    deleteButton.setManaged(isAdmin);
  }

  /** Navigator sets the id after FXML load, so the setter triggers loading. */
  public void setUserId(UUID userId) {
    this.userId = userId;
    if (fullNameLabel != null && userId != null && services != null) {
      load();
    }
  }

  // ===== Loading =====

  private void load() {
    hideError();
    statusLabel.setText("Загрузка…");
    setContentVisible(false);

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
          statusLabel.setText("");
          if (current == null) {
            showError("Пользователь не найден");
            return;
          }
          showUser(current);
          setContentVisible(true);
        });

    task.setOnFailed(
        e -> {
          statusLabel.setText("");
          var ex = task.getException();
          String msg =
              ex instanceof ServiceException se
                  ? se.getMessage()
                  : "Не удалось загрузить пользователя";
          showError(msg);
        });

    startDaemon(task, "user-details-load");
  }

  private void showUser(User user) {
    String first = user.getFirstName() == null ? "" : user.getFirstName();
    String last = user.getLastName() == null ? "" : user.getLastName();
    String full = (first + " " + last).trim();
    fullNameLabel.setText(full.isEmpty() ? "Без имени" : full);

    emailLabel.setText(user.getEmail() == null ? "" : user.getEmail());
    roleLabel.setText(user.getRole() == null ? "" : user.getRole().name());
    enabledLabel.setText(Boolean.TRUE.equals(user.getEnabled()) ? "Да" : "Нет");
    uuidLabel.setText(user.getUuid() == null ? "" : user.getUuid().toString());

    updateDeleteButtonState(user);
  }

  /**
   * Админ не может удалить собственную учётную запись. Кнопка видна только администраторам (см.
   * initialize), но дополнительно гасится, если просматривается именно текущий пользователь.
   */
  private void updateDeleteButtonState(User user) {
    var authService = services == null ? null : services.getAuthService();
    var me = authService == null ? null : authService.getCurrentUser();
    boolean isSelf =
        me != null && user != null && user.getUuid() != null && user.getUuid().equals(me.getUuid());
    deleteButton.setDisable(isSelf);
  }

  // ===== Event handlers =====

  @FXML
  private void onBack(ActionEvent e) {
    navigator.goTo("/fxml/UserList.fxml");
  }

  @FXML
  private void onEdit(ActionEvent e) {
    if (current == null || current.getUuid() == null) {
      return;
    }
    navigator.goTo(
        "/fxml/UserUpdateForm.fxml",
        c -> ((UserUpdateFormController) c).setUserId(current.getUuid()));
  }

  @FXML
  private void onDelete(ActionEvent e) {
    if (current == null || current.getUuid() == null) {
      return;
    }

    // Двойная защита: даже если кнопку как-то активировали, себя удалить нельзя.
    var authService = services == null ? null : services.getAuthService();
    var me = authService == null ? null : authService.getCurrentUser();
    if (me != null && current.getUuid().equals(me.getUuid())) {
      showError("Нельзя удалить собственную учётную запись");
      return;
    }

    if (!confirmDelete(current)) {
      return;
    }

    hideError();
    setBusy(true);

    var uuid = current.getUuid();
    var task =
        new Task<Void>() {
          @Override
          protected Void call() {
            services.getUsersService().delete(uuid);
            return null;
          }
        };

    task.setOnSucceeded(
        ev -> {
          setBusy(false);
          navigator.goTo("/fxml/UserList.fxml");
        });

    task.setOnFailed(
        ev -> {
          setBusy(false);
          var ex = task.getException();
          String msg =
              ex instanceof ServiceException se
                  ? se.getMessage()
                  : "Не удалось удалить пользователя";
          showError(msg);
        });

    startDaemon(task, "user-delete");
  }

  // ===== Confirmation dialog =====

  /**
   * Диалог подтверждения удаления. Выделен в метод ради тестов: headless {@code Alert} блокирует
   * поток, поэтому тесты переопределяют его без UI.
   */
  protected boolean confirmDelete(User user) {
    var alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Удаление пользователя");
    alert.setHeaderText(null);
    alert.setContentText(
        "Удалить пользователя \""
            + (user.getEmail() == null ? user.getUuid().toString() : user.getEmail())
            + "\"? Действие необратимо.");

    var ok = new ButtonType("Удалить", ButtonBar.ButtonData.OK_DONE);
    var cancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
    alert.getButtonTypes().setAll(ok, cancel);

    return alert.showAndWait().filter(ok::equals).isPresent();
  }

  // ===== UI helpers =====

  private void setContentVisible(boolean visible) {
    fullNameLabel.setVisible(visible);
    emailLabel.setVisible(visible);
    roleLabel.setVisible(visible);
    enabledLabel.setVisible(visible);
    uuidLabel.setVisible(visible);
  }

  private void setBusy(boolean busy) {
    deleteButton.setDisable(busy);
    editButton.setDisable(busy);
    backButton.setDisable(busy);
    statusLabel.setText(busy ? "Удаление…" : "");
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

  private static void startDaemon(Task<?> task, String name) {
    var thread = new Thread(task, name);
    thread.setDaemon(true);
    thread.start();
  }
}
