package com.DesktopApplicationClientJava.Api;

import com.DesktopApplicationClientJava.Api.Dto.error.ErrorResponse;
import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
  private final ErrorResponse errorResponse;

  public ApiException(ErrorResponse errorResponse) {
    super(errorResponse.message());
    this.errorResponse = errorResponse;
  }

  public int getStatus() {
    return errorResponse.status();
  }
}
