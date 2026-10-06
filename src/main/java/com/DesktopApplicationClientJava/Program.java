package com.DesktopApplicationClientJava;

import com.DesktopApplicationClientJava.Api.ApiClient;
import com.DesktopApplicationClientJava.navigation.Navigator;
import com.DesktopApplicationClientJava.services.ServiceRegistry;
import com.DesktopApplicationClientJava.session.Session;
import com.DesktopApplicationClientJava.utils.ControllerWiring;
import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;

public class Program extends Application {
  private static final String INITIAL_PAGE_PATH =
      System.getenv().getOrDefault("INITIAL_PAGE_PATH", "/fxml/LoginForm.fxml");
  private static final boolean MOCK_DATA =
      Boolean.parseBoolean(System.getenv().getOrDefault("MOCK_DATA", "false"));
  private static final double WINDOW_WIDTH = 800.0;
  private static final double WINDOW_HEIGHT = 600.0;

  private final Session session = new Session();
  private final ApiClient apiClient = new ApiClient(session);
  private final ServiceRegistry services = createServices();

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) throws IOException {
    Navigator navigator = new Navigator(stage, session, services);

    FXMLLoader loader = new FXMLLoader(getClass().getResource(INITIAL_PAGE_PATH));
    loader.setControllerFactory(
        clazz -> {
          try {
            Object controller = clazz.getDeclaredConstructor().newInstance();
            ControllerWiring.wire(controller, navigator, session, services);
            return controller;
          } catch (Exception e) {
            throw new RuntimeException("Cannot create controller: " + clazz, e);
          }
        });

    Parent root = loader.load();
    Scene scene = new Scene(root);
    scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
    loadStyleSheet(scene, "/styles.css");

    stage.setScene(scene);
    stage.setTitle("JavaFX Application" + (MOCK_DATA ? " [MOCK]" : ""));
    stage.setWidth(WINDOW_WIDTH);
    stage.setHeight(WINDOW_HEIGHT);
    stage.centerOnScreen();
    stage.show();
  }

  private ServiceRegistry createServices() {
    if (MOCK_DATA) {
      return ServiceRegistry.mock(session);
    }
    return new ServiceRegistry(apiClient, session);
  }

  private void loadStyleSheet(Scene scene, String path) {
    var url = getClass().getResource(path);
    if (url != null) {
      scene.getStylesheets().add(url.toExternalForm());
    }
  }
}
