package com.DesktopApplicationClientJava;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class MainController {
    private int times = 0;

    @FXML
    private Button button;

    @FXML
    private void click(ActionEvent event) {
        times++;
        button.setText("You've clicked " + times + " times!");
    }
}
