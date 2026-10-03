package com.DesktopApplicationClientJava.utils;

import com.DesktopApplicationClientJava.navigation.Navigator;
import com.DesktopApplicationClientJava.navigation.NavigatorAware;
import com.DesktopApplicationClientJava.services.ServiceRegistry;
import com.DesktopApplicationClientJava.services.ServicesAware;
import com.DesktopApplicationClientJava.session.Session;
import com.DesktopApplicationClientJava.session.SessionAware;

public final class ControllerWiring {
  public static void wire(
      Object controller, Navigator navigator, Session session, ServiceRegistry services) {
    if (controller instanceof SessionAware sa) sa.setSession(session);
    if (controller instanceof NavigatorAware na) na.setNavigator(navigator);
    if (controller instanceof ServicesAware sv) sv.setServices(services);
  }
}
