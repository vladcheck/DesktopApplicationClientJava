package com.DesktopApplicationClientJava;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Program extends Application {
    final static double windowWidthPx = 500.0;
    final static double windowHeightPx = 500.0;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Text text = new Text("Hello JAVAFX!");
        text.setLayoutX(windowWidthPx / 2);
        text.setLayoutY(windowHeightPx / 2);

        Group group = new Group(text);
        Scene scene = new Scene(group);

        stage.setScene(scene);
        stage.setTitle("JavaFX Application");
        stage.setWidth(windowWidthPx);
        stage.setHeight(windowHeightPx);
        stage.centerOnScreen();

        stage.show();
    }
}