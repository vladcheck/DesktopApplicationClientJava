package com.DesktopApplicationClientJava.utils;

import com.DesktopApplicationClientJava.navigation.Navigator;
import com.DesktopApplicationClientJava.navigation.NavigatorAware;
import com.DesktopApplicationClientJava.session.Session;
import com.DesktopApplicationClientJava.session.SessionAware;
import javafx.stage.Stage;

public final class ControllerWiring {
  private ControllerWiring() {}

  public static void wire(Object controller, Stage stage, Session session) {
    if (controller instanceof SessionAware sa) sa.setSession(session);
    if (controller instanceof NavigatorAware na) na.setNavigator(new Navigator(stage, session));
  }
}
