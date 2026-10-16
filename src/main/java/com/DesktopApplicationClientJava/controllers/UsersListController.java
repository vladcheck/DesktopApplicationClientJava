package com.DesktopApplicationClientJava.controllers;

import com.DesktopApplicationClientJava.controllers.utils.Controller;
import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.services.filter.UserFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

public class UsersListController extends Controller {

  private static final int PAGE_SIZE = 20;

  /** Насколько широко тянем список при активном поиске (клиентская фильтрация). */
  private static final int SEARCH_LIMIT = 500;

  @FXML private Label userInfoLabel;
  @FXML private Label errorLabel;
  @FXML private Label pageInfoLabel;
  @FXML private Label statusLabel;
  @FXML private TextField searchField;
  @FXML private ComboBox<String> sortCombo;
  @FXML private TableView<User> usersTable;
  @FXML private TableColumn<User, String> emailColumn;
  @FXML private TableColumn<User, String> firstNameColumn;
  @FXML private TableColumn<User, String> lastNameColumn;
  @FXML private TableColumn<User, String> roleColumn;
  @FXML private TableColumn<User, String> enabledColumn;
  @FXML private Button createButton;
  @FXML private Button prevButton;
  @FXML private Button nextButton;
  @FXML private Button logoutButton;
  @FXML private Button resourcesButton;

  private final ObservableList<User> rows = FXCollections.observableArrayList();
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
    usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

    if (services != null && services.getUsersService() != null) {
      reloadFromStart();
    }
  }

  // ===== UI setup =====

  private void setupUser() {
    var authService = services == null ? null : services.getAuthService();
    var me = authService == null ? null : authService.getCurrentUser();
    userInfoLabel.setText(
        me == null ? "" : me.getFirstName() + " " + me.getLastName() + " · " + me.getRole());
  }

  private void setupSort() {
    sortCombo
        .getItems()
        .setAll("По email (А-Я)", "По email (Я-А)", "По имени (А-Я)", "По фамилии (А-Я)");
    sortCombo.getSelectionModel().selectFirst();
  }

  @FXML
  private void onOpenResources(ActionEvent e) {
    navigator.goTo("/fxml/ResourceList.fxml");
  }

  private void setupTable() {
    emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
    firstNameColumn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
    lastNameColumn.setCellValueFactory(new PropertyValueFactory<>("lastName"));
    roleColumn.setCellValueFactory(
        c ->
            new SimpleStringProperty(
                c.getValue().getRole() == null ? "" : c.getValue().getRole().name()));
    enabledColumn.setCellValueFactory(
        c ->
            new SimpleStringProperty(
                Boolean.TRUE.equals(c.getValue().getEnabled()) ? "Да" : "Нет"));

    usersTable.setItems(rows);
    usersTable.setPlaceholder(new Label("Пользователей пока нет"));

    usersTable.setRowFactory(
        tv -> {
          var row = new TableRow<User>();
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
    // NB: services/getAuthService может быть частичным моком в тестах — остаёмся null-safe:
    // getAuthService()==null, getCurrentUser()==null и role==null дают скрытые кнопки без NPE,
    // т.к. isAdmin() внутри проверяет current и role на null.
    var authService = services == null ? null : services.getAuthService();
    boolean canCreate = authService != null && authService.isAdmin();
    createButton.setVisible(canCreate);
    createButton.setManaged(canCreate);

    // Кнопка «Ресурсы» доступна только администраторам (как кнопка «Пользователи» на списке
    // ресурсов).
    boolean isAdmin = authService != null && authService.isAdmin();
    resourcesButton.setVisible(isAdmin);
    resourcesButton.setManaged(isAdmin);
  }

  // ===== Data loading =====

  private void reloadFromStart() {
    offset = 0;
    loadAsync();
  }

  private void loadAsync() {
    hideError();
    statusLabel.setText("Загрузка…");

    final String query = currentQuery();
    final boolean searching = query != null;
    final UserFilter filter = buildFilter(query);
    final SortSpec sort = resolveSort();
    final long currentOffset = offset;
    // При активном поиске тянем широкую выборку и фильтруем локально,
    // чтобы не потерять совпадения по имени/фамилии.
    final long fetchCount = searching ? SEARCH_LIMIT : PAGE_SIZE;
    final long fetchOffset = searching ? 0 : currentOffset;

    var task =
        new Task<List<User>>() {
          @Override
          protected List<User> call() {
            return services.getUsersService().search(filter, fetchOffset, fetchCount);
          }
        };

    task.setOnSucceeded(
        e -> {
          List<User> page = task.getValue();
          if (page == null) {
            page = List.of();
          }
          var list = new ArrayList<>(page);

          // Клиентская фильтрация по подстроке (email + имя + фамилия).
          if (searching) {
            list.removeIf(u -> !matchesQuery(u, query));
          }

          sort.sort(list);
          rows.setAll(list);

          updatePaginationUi(searching ? list.size() : page.size(), searching);
          statusLabel.setText(searching ? "Найдено: " + list.size() : "");
        });

    task.setOnFailed(
        e -> {
          var ex = task.getException();
          String msg =
              ex instanceof ServiceException se ? se.getMessage() : "Не удалось загрузить список";
          showError(msg);
          rows.clear();
          updatePaginationUi(0, searching);
          statusLabel.setText("");
        });

    var thread = new Thread(task, "users-list-load");
    thread.setDaemon(true);
    thread.start();
  }

  private String currentQuery() {
    String raw = searchField.getText();
    if (raw == null) {
      return null;
    }
    String trimmed = raw.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  /**
   * Различные бэкенды по-разному комбинируют поля фильтра. Чтобы поиск работал предсказуемо, на
   * сервер отправляем только email-фильтр (когда запрос похож на email), а имя/фамилию фильтруем
   * локально.
   */
  private UserFilter buildFilter(String query) {
    if (query == null) {
      return UserFilter.empty();
    }
    boolean looksLikeEmail = query.contains("@");
    return looksLikeEmail ? new UserFilter(query, null, null, null, null) : UserFilter.empty();
  }

  private static boolean matchesQuery(User u, String query) {
    if (u == null) {
      return false;
    }
    String needle = query.toLowerCase(Locale.ROOT);
    return containsIgnoreCase(u.getEmail(), needle)
        || containsIgnoreCase(u.getFirstName(), needle)
        || containsIgnoreCase(u.getLastName(), needle);
  }

  private static boolean containsIgnoreCase(String value, String lowerNeedle) {
    return value != null && value.toLowerCase(Locale.ROOT).contains(lowerNeedle);
  }

  private record SortSpec(java.util.Comparator<User> comparator) {
    void sort(List<User> list) {
      if (comparator != null) {
        list.sort(comparator);
      }
    }
  }

  private SortSpec resolveSort() {
    String choice = sortCombo.getValue();
    if (choice == null) {
      return new SortSpec(null);
    }
    return switch (choice) {
      case "По email (А-Я)" ->
          new SortSpec(
              java.util.Comparator.comparing(
                  u -> u.getEmail() == null ? "" : u.getEmail(), String.CASE_INSENSITIVE_ORDER));
      case "По email (Я-А)" ->
          new SortSpec(
              java.util.Comparator.comparing(
                      (User u) -> u.getEmail() == null ? "" : u.getEmail(),
                      String.CASE_INSENSITIVE_ORDER)
                  .reversed());
      case "По имени (А-Я)" ->
          new SortSpec(
              java.util.Comparator.comparing(
                  u -> u.getFirstName() == null ? "" : u.getFirstName(),
                  String.CASE_INSENSITIVE_ORDER));
      case "По фамилии (А-Я)" ->
          new SortSpec(
              java.util.Comparator.comparing(
                  u -> u.getLastName() == null ? "" : u.getLastName(),
                  String.CASE_INSENSITIVE_ORDER));
      default -> new SortSpec(null);
    };
  }

  private void updatePaginationUi(int pageSize, boolean searching) {
    if (searching) {
      // При активном поиске мы показываем «плоский» отфильтрованный список
      // без серверной пагинации — кнопки не имеют смысла.
      pageInfoLabel.setText("Показано " + rows.size());
      prevButton.setDisable(true);
      nextButton.setDisable(true);
      return;
    }
    long from = rows.isEmpty() ? 0 : offset + 1;
    long to = offset + pageSize;
    pageInfoLabel.setText("Показано " + from + "–" + to);
    prevButton.setDisable(offset == 0);
    nextButton.setDisable(pageSize < PAGE_SIZE);
  }

  // ===== Event handlers =====

  @FXML
  private void onCreate(ActionEvent e) {
    navigator.goTo("/fxml/UserCreateForm.fxml");
  }

  @FXML
  private void onLogout(ActionEvent e) {
    services.getAuthService().logout();
    navigator.goTo("/fxml/LoginForm.fxml");
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

  private void openDetails(User user) {
    navigator.goTo(
        "/fxml/UserDetails.fxml", c -> ((UserDetailsController) c).setUserId(user.getUuid()));
  }

  // ===== helpers =====

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
