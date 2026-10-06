package com.DesktopApplicationClientJava.controllers.utils;

import com.DesktopApplicationClientJava.navigation.Navigator;
import com.DesktopApplicationClientJava.navigation.NavigatorAware;
import com.DesktopApplicationClientJava.services.ServiceRegistry;
import com.DesktopApplicationClientJava.services.ServicesAware;
import com.DesktopApplicationClientJava.session.Session;
import com.DesktopApplicationClientJava.session.SessionAware;
import lombok.Setter;

@Setter
public abstract class Controller implements SessionAware, NavigatorAware, ServicesAware {

  protected Session session;
  protected Navigator navigator;
  protected ServiceRegistry services;
}
