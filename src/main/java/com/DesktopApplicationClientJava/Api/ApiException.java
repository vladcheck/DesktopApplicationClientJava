package com.DesktopApplicationClientJava.Api;

import dto.Error.ErrorResponse;

public class ApiException extends RuntimeException {
    private final ErrorResponse errorResponse;

    public ApiException(ErrorResponse errorResponse) {
        super(errorResponse.message());
        this.errorResponse = errorResponse;
    }

    public ErrorResponse getErrorResponse() {
        return errorResponse;
    }

    public int getStatus() {
        return errorResponse.status();
    }
}