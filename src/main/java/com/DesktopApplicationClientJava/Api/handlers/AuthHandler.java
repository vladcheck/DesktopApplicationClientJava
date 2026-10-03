package com.DesktopApplicationClientJava.Api.handlers;

import com.DesktopApplicationClientJava.Api.Dto.request.Auth.LoginRequest;
import com.DesktopApplicationClientJava.Api.Dto.request.Auth.RefreshTokenRequest;
import com.DesktopApplicationClientJava.Api.Dto.response.Jwt.JwtTokenResponse;
import com.DesktopApplicationClientJava.Api.HttpController;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AuthHandler extends Handler {
  public AuthHandler(HttpController httpController) {
    super(httpController);
  }

  // Вход в систему
  public JwtTokenResponse login(LoginRequest request) throws IOException, InterruptedException {
    String jsonBody = httpController.objectMapper.writeValueAsString(request);
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(httpController.baseUrl + "/api/v1/auth/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();
    HttpResponse<String> response = httpController.send(httpRequest);
    httpController.checkError(response);
    JwtTokenResponse tokenResponse = httpController.parseResponse(response, JwtTokenResponse.class);
    httpController.accessToken = tokenResponse.accessToken();
    httpController.refreshToken = tokenResponse.refreshToken();
    return tokenResponse;
  }

  // Выход из системы
  public void logout() throws IOException, InterruptedException {
    if (httpController.accessToken == null) {
      return;
    }
    String jsonBody =
        httpController.objectMapper.writeValueAsString(
            new RefreshTokenRequest(httpController.refreshToken));
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(httpController.baseUrl + "/api/v1/auth/logout"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + httpController.accessToken)
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();
    HttpResponse<String> response = httpController.send(httpRequest);
    httpController.checkError(response);
    httpController.accessToken = null;
    httpController.refreshToken = null;
  }
}
