package com.DesktopApplicationClientJava.services;

import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {
  private final int status;

  public ServiceException(String message, int status) {
    super(message);
    this.status = status;
  }

  public ServiceException(String message) {
    this(message, 0);
  }
}
