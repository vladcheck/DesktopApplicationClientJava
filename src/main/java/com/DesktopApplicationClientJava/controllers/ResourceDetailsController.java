package com.DesktopApplicationClientJava.controllers;

import com.DesktopApplicationClientJava.controllers.utils.Controller;
import com.DesktopApplicationClientJava.entities.FileInfo;
import com.DesktopApplicationClientJava.entities.Resource;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.services.filter.ResourceFilter;
import com.DesktopApplicationClientJava.services.mock.MockService;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

public class ResourceDetailsController extends Controller {

  static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

  @FXML Label titleLabel;
  @FXML Label descriptionLabel;
  @FXML Label createdLabel;
  @FXML Label updatedLabel;
  @FXML Label uuidLabel;
  @FXML Label errorLabel;
  @FXML Label statusLabel;
  @FXML TableView<FileInfo> filesTable;
  @FXML TableColumn<FileInfo, String> nameColumn;
  @FXML TableColumn<FileInfo, String> sizeColumn;
  @FXML TableColumn<FileInfo, String> contentTypeColumn;
  @FXML TableColumn<FileInfo, Void> downloadColumn;
  @FXML Button backButton;
  @FXML Button editButton;
  @FXML Button deleteButton;

  @Override
  protected Label getErrorLabel() {
    return errorLabel;
  }

  private UUID resourceId;

  /**
   * Navigator sets the id <i>after</i> FXML load (see {@code Navigator.goTo}), so the setter itself
   * must load the resource when the view is already initialized.
   */
  public void setResourceId(UUID resourceId) {
    this.resourceId = resourceId;
    if (titleLabel != null && resourceId != null && services != null) {
      load();
    }
  }

  @FXML
  private void initialize() {
    hideError();

    nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
    contentTypeColumn.setCellValueFactory(new PropertyValueFactory<>("contentType"));
    sizeColumn.setCellValueFactory(
        c -> new SimpleStringProperty(formatSize(c.getValue().getSize())));
    sizeColumn.setCellFactory(
        col -> {
          TableCell<FileInfo, String> cell =
              new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                  super.updateItem(item, empty);
                  setText(empty ? null : item);
                  setAlignment(Pos.CENTER_RIGHT);
                }
              };
          return cell;
        });

    downloadColumn.setCellFactory(col -> new DownloadCell(this::onDownload));
    filesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    filesTable.setPlaceholder(new Label("У этого документа нет файлов"));
    // NB: services/getAuthService may be a partial mock in tests — stay null-safe.
    var authService = services == null ? null : services.getAuthService();
    boolean canEdit = authService != null && (authService.isAdmin() || authService.isModerator());
    editButton.setVisible(canEdit);
    editButton.setManaged(canEdit);
    deleteButton.setVisible(canEdit);
    deleteButton.setManaged(canEdit);
    autoSelectFirstInMockMode();
  }

  public void load() {
    hideError();
    try {
      showResource(services.getResourceService().getById(resourceId));
    } catch (ServiceException e) {
      showError(e);
    }
  }

  @FXML
  void onBack(ActionEvent event) {
    navigator.goTo("/fxml/ResourceList.fxml");
  }

  @FXML
  void onDownload(FileInfo file) {
    File dir = chooseDirectory(backButton.getScene().getWindow());
    if (dir == null) {
      return;
    }
    try {
      Path saved = services.getFileService().downloadFile(resourceId, file.getUuid(), dir.toPath());
      statusLabel.setText("Сохранено: " + saved.getFileName());
    } catch (ServiceException e) {
      showError(e);
    }
  }

  protected File chooseDirectory(Window owner) {
    return new DirectoryChooser().showDialog(owner);
  }

  /**
   * Dev-only fallback: when the page is opened without an id (e.g. straight via INITIAL_PAGE_PATH)
   * against a mock service, show the first seeded resource so the UI is clickable without the
   * resource list. Never triggers against the real backend.
   */
  private void autoSelectFirstInMockMode() {
    if (resourceId != null
        || services == null
        || services.getResourceService() == null
        || !(services.getResourceService() instanceof MockService mock)) {
      return;
    }
    mock.doIfInitialPage();
    services.getResourceService().search(ResourceFilter.empty(), 0, 1).stream()
        .map(Resource::getUuid)
        .findFirst()
        .ifPresent(this::setResourceId); // the setter loads the resource
  }

  private void showResource(Resource resource) {
    titleLabel.setText(resource.getTitle());
    descriptionLabel.setText(resource.getDescription());
    createdLabel.setText(formatDateTime(resource.getCreatedAt()));
    updatedLabel.setText(formatDateTime(resource.getUpdatedAt()));
    uuidLabel.setText(resource.getUuid() == null ? "" : resource.getUuid().toString());
    List<FileInfo> files = resource.getFiles() == null ? List.of() : resource.getFiles();
    filesTable.getItems().setAll(files);
  }

  private static String formatDateTime(LocalDateTime value) {
    return value == null ? "" : DATE_TIME_FORMAT.format(value);
  }

  private static String formatSize(Long bytes) {
    if (bytes == null) return "";
    if (bytes < 1024) return bytes + " B";
    if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
    if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
    return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
  }

  private static final class DownloadCell extends TableCell<FileInfo, Void> {
    private final Button button = new Button("Скачать");

    DownloadCell(Consumer<FileInfo> onDownload) {
      button.getStyleClass().setAll("btn-ghost");
      button.setOnAction(event -> onDownload.accept(getTableView().getItems().get(getIndex())));
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
      super.updateItem(item, empty);
      setGraphic(empty ? null : button);
    }
  }

  @FXML
  private void onEdit(ActionEvent event) {
    if (resourceId == null) return;
    navigator.goTo(
        "/fxml/ResourceForm.fxml", c -> ((ResourceFormController) c).setResourceId(resourceId));
  }

  @FXML
  void onDelete(ActionEvent event) {
    if (resourceId == null) return;
    if (!confirmDelete()) return;
    try {
      services.getResourceService().delete(resourceId);
    } catch (ServiceException e) {
      showError(e);
      return;
    }
    navigator.goTo("/fxml/ResourceList.fxml");
  }

  /**
   * Диалог подтверждения удаления. Выделен в метод ради тестов: headless {@code Alert} блокирует
   * поток, поэтому тесты переопределяют его без UI.
   */
  protected boolean confirmDelete() {
    var alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Удаление ресурса");
    alert.setHeaderText("Удалить ресурс?");
    alert.setContentText("Ресурс будет удалён без возможности восстановления.");
    return alert.showAndWait().map(button -> button == ButtonType.OK).orElse(false);
  }
}
