package com.DesktopApplicationClientJava;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Program extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Text text = new Text("Hello JAVAFX!");
        text.setLayoutX(80);
        text.setLayoutY(80);

        Group group = new Group(text);
        Scene scene = new Scene(group);

        stage.setScene(scene);
        stage.setTitle("JavaFX Application");
        stage.setWidth(300);
        stage.setHeight(250);

        stage.show();
    }
}