package com.DesktopApplicationClientJava.navigation;

import com.DesktopApplicationClientJava.services.AuthService;
import com.DesktopApplicationClientJava.services.ServiceRegistry;
import com.DesktopApplicationClientJava.session.Session;
import com.DesktopApplicationClientJava.session.SessionAware;
import com.DesktopApplicationClientJava.utils.ControllerWiring;
import java.io.IOException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;

/** Navigator */
public class Navigator {
  private static final Logger LOG = Logger.getLogger(Navigator.class.getName());

  private static final String LOGIN = "/fxml/LoginForm.fxml";
  private static final String FALLBACK = "/fxml/ResourceList.fxml";

  /** Страницы пользователей доступны только администраторам. */
  private static final Set<String> ADMIN_ONLY =
      Set.of(
          "/fxml/UserList.fxml",
          "/fxml/UserDetails.fxml",
          "/fxml/UserCreateForm.fxml",
          "/fxml/UserUpdateForm.fxml");

  /** Форма ресурса доступна администраторам и модераторам. */
  private static final Set<String> MODER_PLUS = Set.of("/fxml/ResourceForm.fxml");

  private final Stage stage;
  private final Session session;
  private final ServiceRegistry services;

  public Navigator(Stage stage, Session session, ServiceRegistry services) {
    this.stage = stage;
    this.session = session;
    this.services = services;
  }

  public void goTo(String fxmlFile) {
    goTo(fxmlFile, controller -> {});
  }

  public void goTo(String fxmlFile, Consumer<Object> withController) {
    String redirect = guardRedirect(fxmlFile);
    if (redirect != null) {
      if (redirect.equals(fxmlFile)) {
        load(fxmlFile, withController);
      } else {
        LOG.fine(() -> "Переход " + fxmlFile + " отклонён, редирект на " + redirect);
        load(redirect, controller -> {});
      }
      return;
    }
    load(fxmlFile, withController);
  }

  /**
   * Клиентский гард по роли. Возвращает null, если переход разрешён, иначе целевой FXML: гость (нет
   * пользователя) — форма входа, залогиненный без роли — список ресурсов. Fail-closed: null
   * сессии/AuthService — на вход.
   */
  private String guardRedirect(String fxmlFile) {
    boolean adminPage = ADMIN_ONLY.contains(fxmlFile);
    boolean moderPage = MODER_PLUS.contains(fxmlFile);
    if (!adminPage && !moderPage) {
      return null;
    }
    AuthService auth = null;
    try {
      auth = services == null ? null : services.getAuthService();
    } catch (RuntimeException e) {
      LOG.fine(() -> "Гард: AuthService недоступен, редирект на вход");
      return LOGIN;
    }
    if (session == null || auth == null) {
      return LOGIN;
    }
    Object current;
    try {
      current = auth.getCurrentUser();
    } catch (RuntimeException e) {
      return LOGIN;
    }
    if (current == null) {
      return LOGIN;
    }
    boolean allowed;
    try {
      allowed = adminPage ? auth.isAdmin() : (auth.isAdmin() || auth.isModerator());
    } catch (RuntimeException e) {
      return LOGIN;
    }
    if (allowed) {
      return null;
    }
    return FALLBACK;
  }

  private void load(String fxmlFile, Consumer<Object> withController) {
    var url = Navigator.class.getResource(fxmlFile);
    if (url == null) {
      throw new IllegalArgumentException("FXML not found on classpath: " + fxmlFile);
    }

    FXMLLoader loader = new FXMLLoader(url);
    loader.setControllerFactory(
        clazz -> {
          try {
            Object controller = clazz.getDeclaredConstructor().newInstance();
            ControllerWiring.wire(controller, this, session, services);
            return controller;
          } catch (Exception e) {
            throw new RuntimeException("Cannot create controller: " + clazz, e);
          }
        });

    try {
      Parent root = loader.load();

      Object controller = loader.getController();
      if (controller instanceof SessionAware sa) {
        sa.setSession(session);
      }
      if (controller instanceof NavigatorAware na) {
        na.setNavigator(this);
      }
      withController.accept(controller);

      stage.getScene().setRoot(root);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load FXML: " + fxmlFile, e);
    }
  }
}
