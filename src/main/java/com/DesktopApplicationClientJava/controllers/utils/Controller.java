package com.DesktopApplicationClientJava.controllers.utils;

import com.DesktopApplicationClientJava.Api.ApiException;
import com.DesktopApplicationClientJava.navigation.Navigator;
import com.DesktopApplicationClientJava.navigation.NavigatorAware;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.services.ServiceRegistry;
import com.DesktopApplicationClientJava.services.ServicesAware;
import com.DesktopApplicationClientJava.session.Session;
import com.DesktopApplicationClientJava.session.SessionAware;
import javafx.scene.control.Label;
import lombok.Setter;

@Setter
public abstract class Controller implements SessionAware, NavigatorAware, ServicesAware {

  protected Session session;
  protected Navigator navigator;
  protected ServiceRegistry services;

  protected abstract Label getErrorLabel();

  protected void showError(String message) {
    Label errorLabel = getErrorLabel();
    if (errorLabel == null) {
      return;
    }
    errorLabel.setText(message == null ? "" : message);
    errorLabel.setVisible(true);
    errorLabel.setManaged(true);
  }

  protected void hideError() {
    Label errorLabel = getErrorLabel();
    if (errorLabel == null) {
      return;
    }
    errorLabel.setText("");
    errorLabel.setVisible(false);
    errorLabel.setManaged(false);
  }

  /**
   * Хук перехвата 401/403. Форма входа перекрывает в {@code false}: неверный пароль остаётся на
   * месте с текстом ошибки без выхода.
   */
  protected boolean interceptAuthFailure() {
    return true;
  }

  protected void showError(Throwable ex) {
    showError(ex, ex == null ? "Произошла ошибка" : ex.getMessage());
  }

  protected void showError(Throwable ex, String fallback) {
    if (interceptAuthFailure() && isAuthFailure(ex)) {
      if (session != null) {
        session.clear();
      }
      if (navigator != null) {
        navigator.goToLoginOnce();
      }
      return;
    }
    String message = extractMessage(ex, fallback);
    showError(message);
  }

  private static boolean isAuthFailure(Throwable ex) {
    for (Throwable current = ex; current != null; current = current.getCause()) {
      if (current instanceof ServiceException se
          && (se.getStatus() == 401 || se.getStatus() == 403)) {
        return true;
      }
      if (current instanceof ApiException ae && (ae.getStatus() == 401 || ae.getStatus() == 403)) {
        return true;
      }
    }
    return false;
  }

  private static String extractMessage(Throwable ex, String fallback) {
    for (Throwable current = ex; current != null; current = current.getCause()) {
      if (current instanceof ServiceException se && se.getMessage() != null) {
        return se.getMessage();
      }
      if (current instanceof ApiException ae
          && ae.getErrorResponse() != null
          && ae.getErrorResponse().message() != null) {
        return ae.getErrorResponse().message();
      }
    }
    return fallback == null ? "Произошла ошибка" : fallback;
  }
}
