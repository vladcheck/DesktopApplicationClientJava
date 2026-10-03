package com.DesktopApplicationClientJava.navigation;

import com.DesktopApplicationClientJava.services.ServiceRegistry;
import com.DesktopApplicationClientJava.session.Session;
import com.DesktopApplicationClientJava.session.SessionAware;
import com.DesktopApplicationClientJava.utils.ControllerWiring;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;

/** Navigator */
public class Navigator {
  private final Stage stage;
  private final Session session;
  private final ServiceRegistry services;

  public Navigator(Stage stage, Session session, ServiceRegistry services) {
    this.stage = stage;
    this.session = session;
    this.services = services;
  }

  public void goTo(String fxmlFile) {
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

      stage.getScene().setRoot(root);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load FXML: " + fxmlFile, e);
    }
  }
}
