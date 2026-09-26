package com.DesktopApplicationClientJava;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Program extends Application {
    final static double windowWidthPx = 200.0;
    final static double windowHeightPx = 100.0;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("Main.fxml"));
        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.setTitle("JavaFX Application");
        stage.setWidth(windowWidthPx);
        stage.setHeight(windowHeightPx);
        stage.centerOnScreen();

        stage.show();
    }
}
