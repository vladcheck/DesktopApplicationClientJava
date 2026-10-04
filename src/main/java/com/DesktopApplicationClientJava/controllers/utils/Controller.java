package com.DesktopApplicationClientJava.controllers.utils;

import com.DesktopApplicationClientJava.navigation.Navigator;
import com.DesktopApplicationClientJava.navigation.NavigatorAware;
import com.DesktopApplicationClientJava.services.ServiceRegistry;
import com.DesktopApplicationClientJava.services.ServicesAware;
import com.DesktopApplicationClientJava.session.Session;
import com.DesktopApplicationClientJava.session.SessionAware;

public abstract class Controller implements SessionAware, NavigatorAware, ServicesAware {

  protected Session session;
  protected Navigator navigator;
  protected ServiceRegistry services;

  @Override
  public void setSession(Session session) {
    this.session = session;
  }

  @Override
  public void setNavigator(Navigator navigator) {
    this.navigator = navigator;
  }

  @Override
  public void setServices(ServiceRegistry services) {
    this.services = services;
  }
}
