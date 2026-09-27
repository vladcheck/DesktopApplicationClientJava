package com.DesktopApplicationClientJava;

import javafx.scene.control.Label;

public class FieldError {
    private final Label label;

    public FieldError(Label label) {
        this.label = label;
    }

    public void show(String message) {
        if (label == null)
            return;
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    public void hide() {
        if (label == null)
            return;
        label.setVisible(false);
        label.setManaged(false);
    }

    public boolean isShowing() {
        return label.isVisible();
    }
}