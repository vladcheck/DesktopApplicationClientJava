package com.DesktopApplicationClientJava.controllers;

import com.DesktopApplicationClientJava.controllers.utils.Controller;
import com.DesktopApplicationClientJava.entities.Resource;
import com.DesktopApplicationClientJava.services.filter.ResourceFilter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class ResourceListController extends Controller {

  private static final DateTimeFormatter DATE_TIME_FORMAT =
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
  private static final int PAGE_SIZE = 20;

  @FXML private Label userInfoLabel;
  @FXML private Label errorLabel;
  @FXML private Label pageInfoLabel;
  @FXML private Label statusLabel;
  @FXML private TextField searchField;
  @FXML private TextField descriptionField;
  @FXML private ComboBox<String> sortCombo;
  @FXML private TableView<Resource> resourceTable;
  @FXML private TableColumn<Resource, String> titleColumn;
  @FXML private TableColumn<Resource, String> descriptionColumn;
  @FXML private TableColumn<Resource, String> createdColumn;
  @FXML private TableColumn<Resource, String> updatedColumn;
  @FXML private Button createButton;
  @FXML private Button deleteButton;
  @FXML private Button prevButton;
  @FXML private Button nextButton;
  @FXML private Button logoutButton;
  @FXML private Button usersButton;

  private final ObservableList<Resource> rows = FXCollections.observableArrayList();
  private long offset = 0;
  private boolean canModify = false;

  @Override
  protected Label getErrorLabel() {
    return errorLabel;
  }

  // ===== Lifecycle =====

  @FXML
  private void initialize() {
    hideError();
    setupUser();
    setupSort();
    setupTable();
    setupRbac();

    searchField.setOnAction(e -> reloadFromStart());
    descriptionField.setOnAction(e -> reloadFromStart());
    sortCombo.setOnAction(e -> reloadFromStart());
    resourceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

    if (services != null && services.getResourceService() != null) {
      reloadFromStart();
    }
  }

  // ===== UI setup =====

  private void setupUser() {
    // NB: services/getAuthService may be a partial mock in tests — stay null-safe.
    var authService = services == null ? null : services.getAuthService();
    var me = authService == null ? null : authService.getCurrentUser();
    userInfoLabel.setText(
        me == null ? "" : me.getFirstName() + " " + me.getLastName() + " · " + me.getRole());
  }

  private void setupSort() {
    sortCombo
        .getItems()
        .setAll("Сначала новые", "Сначала старые", "По названию (А-Я)", "По названию (Я-А)");
    sortCombo.getSelectionModel().selectFirst();
  }

  private void setupTable() {
    titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
    descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
    createdColumn.setCellValueFactory(
        c -> new SimpleStringProperty(formatDate(c.getValue().getCreatedAt())));
    updatedColumn.setCellValueFactory(
        c -> new SimpleStringProperty(formatDate(c.getValue().getUpdatedAt())));

    resourceTable.setItems(rows);
    resourceTable.setPlaceholder(new Label("Документов пока нет"));
    resourceTable
        .getSelectionModel()
        .selectedItemProperty()
        .addListener((obs, oldSel, newSel) -> updateDeleteButtonState());

    resourceTable.setRowFactory(
        tv -> {
          var row = new TableRow<Resource>();
          row.setOnMouseClicked(
              e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                  openDetails(row.getItem());
                }
              });
          return row;
        });
  }

  private void setupRbac() {
    var authService = services == null ? null : services.getAuthService();
    canModify = authService != null && (authService.isAdmin() || authService.isModerator());
    createButton.setVisible(canModify);
    createButton.setManaged(canModify);
    deleteButton.setVisible(canModify);
    deleteButton.setManaged(canModify);
    updateDeleteButtonState();

    // Кнопка «Пользователи» доступна только администраторам.
    boolean isAdmin = authService != null && authService.isAdmin();
    usersButton.setVisible(isAdmin);
    usersButton.setManaged(isAdmin);
  }

  private void updateDeleteButtonState() {
    boolean hasSelection =
        resourceTable != null && resourceTable.getSelectionModel().getSelectedItem() != null;
    deleteButton.setDisable(!(canModify && hasSelection));
  }

  @FXML
  private void onOpenUsers(ActionEvent e) {
    var authService = services == null ? null : services.getAuthService();
    if (authService == null || !authService.isAdmin()) {
      return;
    }
    navigator.goTo("/fxml/UserList.fxml");
  }

  // ===== Data loading =====

  private void reloadFromStart() {
    offset = 0;
    loadAsync();
  }

  private void loadAsync() {
    hideError();
    statusLabel.setText("Загрузка…");

    var filter = buildFilter();
    var sort = resolveSort();
    long currentOffset = offset;

    var task =
        new Task<List<Resource>>() {
          @Override
          protected List<Resource> call() {
            return services
                .getResourceService()
                .search(filter, currentOffset, PAGE_SIZE, sort.sortBy(), sort.sortDir());
          }
        };

    task.setOnSucceeded(
        e -> {
          var page = task.getValue();
          rows.setAll(page);
          updatePaginationUi(page.size());
          statusLabel.setText("");
        });

    task.setOnFailed(
        e -> {
          var ex = task.getException();
          showError(ex, "Не удалось загрузить список");
          rows.clear();
          updatePaginationUi(0);
          statusLabel.setText("");
        });

    var thread = new Thread(task, "resource-list-load");
    thread.setDaemon(true);
    thread.start();
  }

  private record SortSpec(String sortBy, String sortDir) {}

  private ResourceFilter buildFilter() {
    return new ResourceFilter(normalizeQuery(searchField), normalizeQuery(descriptionField));
  }

  private static String normalizeQuery(TextField field) {
    if (field == null || field.getText() == null || field.getText().isBlank()) {
      return null;
    }
    return field.getText().trim();
  }

  private SortSpec resolveSort() {
    String choice = sortCombo.getValue();
    if (choice == null) return new SortSpec("createdAt", "desc");
    return switch (choice) {
      case "Сначала старые" -> new SortSpec("createdAt", "asc");
      case "По названию (А-Я)" -> new SortSpec("title", "asc");
      case "По названию (Я-А)" -> new SortSpec("title", "desc");
      default -> new SortSpec("createdAt", "desc");
    };
  }

  private void updatePaginationUi(int pageSize) {
    long from = rows.isEmpty() ? 0 : offset + 1;
    long to = offset + pageSize;
    pageInfoLabel.setText("Показано " + from + "–" + to);
    prevButton.setDisable(offset == 0);
    nextButton.setDisable(pageSize < PAGE_SIZE);
  }

  // ===== Event handlers =====

  @FXML
  private void onCreate(ActionEvent e) {
    navigator.goTo("/fxml/ResourceForm.fxml");
  }

  @FXML
  void onDelete(ActionEvent e) {
    var selected = resourceTable.getSelectionModel().getSelectedItem();
    if (selected == null) {
      return;
    }
    if (!confirmDelete(selected)) {
      return;
    }
    deleteAsync(selected);
  }

  private void deleteAsync(Resource resource) {
    hideError();
    statusLabel.setText("Удаление…");

    var task =
        new Task<Void>() {
          @Override
          protected Void call() {
            services.getResourceService().delete(resource.getUuid());
            return null;
          }
        };

    task.setOnSucceeded(e -> loadAsync());

    task.setOnFailed(
        e -> {
          var ex = task.getException();
          showError(ex, "Не удалось удалить ресурс");
          statusLabel.setText("");
        });

    var thread = new Thread(task, "resource-list-delete");
    thread.setDaemon(true);
    thread.start();
  }

  /**
   * Диалог подтверждения удаления. Выделен в метод ради тестов: headless {@code Alert} блокирует
   * поток, поэтому тесты переопределяют его без UI.
   */
  protected boolean confirmDelete(Resource resource) {
    var alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Удаление ресурса");
    alert.setHeaderText("Удалить ресурс?");
    alert.setContentText(
        "«" + resource.getTitle() + "» будет удалён без возможности восстановления.");
    return alert.showAndWait().map(button -> button == ButtonType.OK).orElse(false);
  }

  @FXML
  private void onLogout(ActionEvent e) {
    try {
      services.getAuthService().logout();
    } catch (RuntimeException ignored) {
      // Выход съедает ошибку сервиса без мелькания: редирект ниже выполняется в любом случае.
    } finally {
      if (session != null) {
        session.clear();
      }
      navigator.goToLoginOnce();
    }
  }

  @FXML
  private void onSearch(ActionEvent e) {
    offset = 0;
    loadAsync();
  }

  @FXML
  private void onSort(ActionEvent e) {
    offset = 0;
    loadAsync();
  }

  @FXML
  private void onNext(ActionEvent e) {
    offset += PAGE_SIZE;
    loadAsync();
  }

  @FXML
  private void onPrev(ActionEvent e) {
    offset = Math.max(0, offset - PAGE_SIZE);
    loadAsync();
  }

  private void openDetails(Resource resource) {
    navigator.goTo(
        "/fxml/ResourceDetails.fxml",
        c -> ((ResourceDetailsController) c).setResourceId(resource.getUuid()));
  }

  // ===== helpers =====

  private static String formatDate(LocalDateTime value) {
    return value == null ? "" : DATE_TIME_FORMAT.format(value);
  }
}
