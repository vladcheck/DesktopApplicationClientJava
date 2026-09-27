package com.DesktopApplicationClientJava;

import java.io.IOException;

import com.DesktopApplicationClientJava.session.Session;
import com.DesktopApplicationClientJava.utils.ControllerWiring;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Program extends Application {
    private final Session session = new Session();
    private final static double windowWidthPx = 400.0;
    private final static double windowHeightPx = 400.0;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginForm.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        loadStyleSheet(scene, "/styles.css");

        ControllerWiring.wire(loader.getController(), stage, session);

        stage.setScene(scene);
        stage.setTitle("JavaFX Application");
        stage.setWidth(windowWidthPx);
        stage.setHeight(windowHeightPx);
        stage.centerOnScreen();

        stage.show();
    }

    private void loadStyleSheet(Scene scene, String path) {
        scene.getStylesheets().add(getClass().getResource(path).toExternalForm());
    }
}
