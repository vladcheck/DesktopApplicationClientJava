package com.DesktopApplicationClientJava.navigation;

import java.io.IOException;

import com.DesktopApplicationClientJava.session.Session;
import com.DesktopApplicationClientJava.session.SessionAware;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import lombok.AllArgsConstructor;

/**
 * Navigator
 */
@AllArgsConstructor
public class Navigator {
    private final Stage stage;
    private final Session session;

    public void goTo(String fxmlFile) {
        var url = Navigator.class.getResource(fxmlFile);
        if (url == null) {
            throw new IllegalArgumentException("FXML not found on classpath: " + fxmlFile);
        }

        FXMLLoader loader = new FXMLLoader(url);
        try {
            Parent root = loader.load();
            var controller = loader.getController();
            if (controller instanceof SessionAware sa) {
                sa.setSession(session);
            }
            if (controller instanceof NavigatorAware na) {
                na.setNavigator(this);
            }
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML: " + fxmlFile, e);
        }

    }
}
