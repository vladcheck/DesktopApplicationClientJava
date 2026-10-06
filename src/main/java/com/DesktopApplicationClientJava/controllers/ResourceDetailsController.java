package com.DesktopApplicationClientJava.controllers;

import com.DesktopApplicationClientJava.controllers.utils.Controller;
import com.DesktopApplicationClientJava.entities.FileInfo;
import com.DesktopApplicationClientJava.entities.Resource;
import com.DesktopApplicationClientJava.services.ServiceException;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

public class ResourceDetailsController extends Controller {

  static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

  @FXML private Label titleLabel;
  @FXML private Label descriptionLabel;
  @FXML private Label createdLabel;
  @FXML private Label updatedLabel;
  @FXML private Label uuidLabel;
  @FXML private Label errorLabel;
  @FXML private Label statusLabel;
  @FXML private TableView<FileInfo> filesTable;
  @FXML private TableColumn<FileInfo, String> nameColumn;
  @FXML private TableColumn<FileInfo, Long> sizeColumn;
  @FXML private TableColumn<FileInfo, String> contentTypeColumn;
  @FXML private TableColumn<FileInfo, Void> downloadColumn;
  @FXML private Button backButton;

  private UUID resourceId;

  @FXML
  private void initialize() {
    hideError();
    nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
    sizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));
    contentTypeColumn.setCellValueFactory(new PropertyValueFactory<>("contentType"));
    downloadColumn.setCellFactory(col -> new DownloadCell(this::onDownload));
  }

  public void setResourceId(UUID resourceId) {
    this.resourceId = resourceId;
  }

  public void load() {
    hideError();
    try {
      showResource(services.getResourceService().getById(resourceId));
    } catch (ServiceException e) {
      showError(e.getMessage());
    }
  }

  @FXML
  private void onBack(ActionEvent event) {
    navigator.goTo("/fxml/ResourceList.fxml");
  }

  private void onDownload(FileInfo file) {
    File dir = chooseDirectory(backButton.getScene().getWindow());
    if (dir == null) {
      return;
    }
    try {
      Path saved = services.getFileService().downloadFile(resourceId, file.getUuid(), dir.toPath());
      statusLabel.setText("Сохранено: " + saved.getFileName());
    } catch (ServiceException e) {
      showError(e.getMessage());
    }
  }

  protected File chooseDirectory(Window owner) {
    return new DirectoryChooser().showDialog(owner);
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

  private static final class DownloadCell extends TableCell<FileInfo, Void> {
    private final Button button = new Button("Скачать");

    DownloadCell(Consumer<FileInfo> onDownload) {
      button.getStyleClass().setAll("btn", "btn-default");
      button.setOnAction(event -> onDownload.accept(getTableView().getItems().get(getIndex())));
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
      super.updateItem(item, empty);
      setGraphic(empty ? null : button);
    }
  }
}
