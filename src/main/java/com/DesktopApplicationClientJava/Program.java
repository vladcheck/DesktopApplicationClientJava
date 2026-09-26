package com.DesktopApplicationClientJava;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Program extends Application {
    final static double windowWidthPx = 400.0;
    final static double windowHeightPx = 300.0;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("fxml/LoginForm.fxml"));
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle("JavaFX Application");
        stage.setWidth(windowWidthPx);
        stage.setHeight(windowHeightPx);
        stage.centerOnScreen();

        stage.show();
    }
}
