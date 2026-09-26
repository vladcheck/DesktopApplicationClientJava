package com.DesktopApplicationClientJava;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Program extends Application {
    final static double windowWidthPx = 200.0;
    final static double windowHeightPx = 100.0;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Text text = new Text("Hello JAVAFX!");
        text.setLayoutX(windowWidthPx / 2);
        text.setLayoutY(windowHeightPx / 2);

        Button button = new Button("Button");
        Group group = new Group(button);

        FlowPane root = new FlowPane(text, group);
        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.setTitle("JavaFX Application");
        stage.setWidth(windowWidthPx);
        stage.setHeight(windowHeightPx);
        stage.centerOnScreen();

        stage.show();
    }
}