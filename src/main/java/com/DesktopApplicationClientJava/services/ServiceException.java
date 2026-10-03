package com.DesktopApplicationClientJava.services;

public class ServiceException extends RuntimeException {
    private final int status;

    public ServiceException(String message, int status) {
        super(message);
        this.status = status;
    }

    public ServiceException(String message) {
        this(message, 0);
    }

    public int getStatus() {
        return status;
    }
}