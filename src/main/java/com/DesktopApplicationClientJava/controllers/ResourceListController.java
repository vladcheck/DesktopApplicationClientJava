package com.DesktopApplicationClientJava.controllers;

import com.DesktopApplicationClientJava.controllers.utils.Controller;
import com.DesktopApplicationClientJava.entities.Resource;
import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.services.filter.ResourceFilter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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
    @FXML private ComboBox<String> sortCombo;
    @FXML private TableView<Resource> resourceTable;
    @FXML private TableColumn<Resource, String> titleColumn;
    @FXML private TableColumn<Resource, String> descriptionColumn;
    @FXML private TableColumn<Resource, String> createdColumn;
    @FXML private TableColumn<Resource, String> updatedColumn;
    @FXML private Button createButton;
    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Button logoutButton;   // ← добавь, если ещё нет

    private final ObservableList<Resource> rows = FXCollections.observableArrayList();
    private long offset = 0;

    // ===== Lifecycle =====

    @FXML
    private void initialize() {
        hideError();
        setupUser();
        setupSort();
        setupTable();
        setupRbac();

        searchField.setOnAction(e -> reloadFromStart());
        sortCombo.setOnAction(e -> reloadFromStart());
        resourceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    @Override
    public void onLoad() {
        // Стартовая загрузка — после wiring и построения сцены
        reloadFromStart();
    }

    // ===== UI setup =====

    private void setupUser() {
        var me = services.getAuthService().getCurrentUser();
        userInfoLabel.setText(me == null
                ? ""
                : me.getFirstName() + " " + me.getLastName() + " · " + me.getRole());
    }

    private void setupSort() {
        sortCombo.getItems().setAll(
                "Сначала новые",
                "Сначала старые",
                "По названию (А-Я)",
                "По названию (Я-А)");
        sortCombo.getSelectionModel().selectFirst();
    }

    private void setupTable() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        createdColumn.setCellValueFactory(c ->
                new SimpleStringProperty(formatDate(c.getValue().getCreatedAt())));
        updatedColumn.setCellValueFactory(c ->
                new SimpleStringProperty(formatDate(c.getValue().getUpdatedAt())));

        resourceTable.setItems(rows);
        resourceTable.setPlaceholder(new Label("Документов пока нет"));

        resourceTable.setRowFactory(tv -> {
            var row = new TableRow<Resource>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openDetails(row.getItem());
                }
            });
            return row;
        });
    }

    private void setupRbac() {
        boolean canCreate = services.getAuthService().isAdmin()
                || services.getAuthService().isModerator();
        createButton.setVisible(canCreate);
        createButton.setManaged(canCreate);
    }

    // ===== Data loading =====

    private void reloadFromStart() {
        offset = 0;
        loadAsync();
    }

    private void loadAsync() {
        hideError();
        statusLabel.setText("Загрузка…");

        var filter = new ResourceFilter(
                searchField.getText() == null || searchField.getText().isBlank()
                        ? null
                        : searchField.getText().trim(),
                null);
        var sort = resolveSort();
        long currentOffset = offset;

        var task = new Task<List<Resource>>() {
            @Override
            protected List<Resource> call() {
                return services.getResourceService()
                        .search(filter, currentOffset, PAGE_SIZE, sort.sortBy(), sort.sortDir());
            }
        };

        task.setOnSucceeded(e -> {
            var page = task.getValue();
            rows.setAll(page);
            updatePaginationUi(page.size());
            statusLabel.setText("");
        });

        task.setOnFailed(e -> {
            var ex = task.getException();
            String msg = ex instanceof ServiceException se
                    ? se.getMessage()
                    : "Не удалось загрузить список";
            showError(msg);
            rows.clear();
            updatePaginationUi(0);
            statusLabel.setText("");
        });

        var thread = new Thread(task, "resource-list-load");
        thread.setDaemon(true);
        thread.start();
    }

    private record SortSpec(String sortBy, String sortDir) {}

    private SortSpec resolveSort() {
        String choice = sortCombo.getValue();
        if (choice == null) return new SortSpec("createdAt", "desc");
        return switch (choice) {
            case "Сначала старые"      -> new SortSpec("createdAt", "asc");
            case "По названию (А-Я)"   -> new SortSpec("title", "asc");
            case "По названию (Я-А)"   -> new SortSpec("title", "desc");
            default                    -> new SortSpec("createdAt", "desc");
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
        // Страница создания — отдельная задача; пока просто заглушка
        statusLabel.setText("Раздел создания ещё не реализован");
    }

    @FXML
    private void onLogout(ActionEvent e) {
        services.getAuthService().logout();
        navigator.goTo("/fxml/LoginForm.fxml");
    }

    @FXML private void onSearch(ActionEvent e) { offset = 0; loadAsync(); }
    @FXML private void onSort(ActionEvent e)   { offset = 0; loadAsync(); }
    @FXML private void onNext(ActionEvent e)   { offset += PAGE_SIZE; loadAsync(); }
    @FXML private void onPrev(ActionEvent e)   { offset = Math.max(0, offset - PAGE_SIZE); loadAsync(); }

    private void openDetails(Resource resource) {
        navigator.goTo(
                "/fxml/ResourceDetails.fxml",
                c -> ((ResourceDetailsController) c).setResourceId(resource.getUuid()));
    }

    // ===== helpers =====

    private static String formatDate(LocalDateTime value) {
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
}