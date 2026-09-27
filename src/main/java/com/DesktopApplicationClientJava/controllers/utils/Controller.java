package com.DesktopApplicationClientJava.controllers.utils;

import com.DesktopApplicationClientJava.navigation.Navigator;
import com.DesktopApplicationClientJava.navigation.NavigatorAware;
import com.DesktopApplicationClientJava.session.Session;
import com.DesktopApplicationClientJava.session.SessionAware;

public abstract class Controller implements SessionAware, NavigatorAware {
    protected Session session;
    protected Navigator navigator;

    public void setSession(Session session) {
        this.session = session;
    }

    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }
}
