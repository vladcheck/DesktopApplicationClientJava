package com.DesktopApplicationClientJava.Api;

import com.DesktopApplicationClientJava.Api.Dto.error.ErrorResponse;

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