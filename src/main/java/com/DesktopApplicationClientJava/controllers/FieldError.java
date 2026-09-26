package com.DesktopApplicationClientJava.controllers;

import javafx.scene.control.Label;

class FieldError {
    private final Label label;

    FieldError(Label label) {
        this.label = label;
    }

    void show(String message) {
        if (label == null)
            return;
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    void hide() {
        if (label == null)
            return;
        label.setVisible(false);
        label.setManaged(false);
    }

    boolean isShowing() {
        return label.isVisible();
    }
}