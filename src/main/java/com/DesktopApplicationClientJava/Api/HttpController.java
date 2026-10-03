package com.DesktopApplicationClientJava.Api;

import com.DesktopApplicationClientJava.Api.Dto.error.ErrorResponse;
import com.DesktopApplicationClientJava.Api.Dto.request.Auth.RefreshTokenRequest;
import com.DesktopApplicationClientJava.Api.Dto.response.Jwt.JwtTokenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.AllArgsConstructor;
import lombok.Setter;

@Setter
@AllArgsConstructor
public class HttpController {
  public final String baseUrl;
  public final HttpClient httpClient;
  public final ObjectMapper objectMapper;
  public String refreshToken;
  public String accessToken;

  // Парсер ответа
  public <T> T parseResponse(HttpResponse<String> response, Class<T> clazz) throws IOException {
    if (response.body() == null || response.body().isBlank()) {
      return null;
    }
    return objectMapper.readValue(response.body(), clazz);
  }

  public HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
    return executeWithRetry(request, HttpResponse.BodyHandlers.ofString());
  }

  public <T> HttpResponse<T> executeWithRetry(
      HttpRequest request, HttpResponse.BodyHandler<T> handler)
      throws IOException, InterruptedException {

    HttpResponse<T> response = httpClient.send(request, handler);

    if (response.statusCode() != 401) return response;

    if (request.uri().getPath().startsWith("/api/v1/auth/")) return response;

    if (refreshToken == null) return response;

    try {
      refreshInternal();
    } catch (Exception e) {
      // refresh не удался — отдаём оригинальный 401, вызывающий код разберётся
      return response;
    }

    // повторяем исходный запрос с новым access-токеном
    HttpRequest retried =
        HttpRequest.newBuilder(request, (name, value) -> !name.equalsIgnoreCase("Authorization"))
            .header("Authorization", "Bearer " + accessToken)
            .build();

    return httpClient.send(retried, handler);
  }

  // Проверка ошибок сервера
  public void checkError(HttpResponse<String> response) {
    if (response.statusCode() < 400) return;
    try {
      ErrorResponse error = objectMapper.readValue(response.body(), ErrorResponse.class);
      throw new ApiException(error);
    } catch (ApiException e) {
      throw e;
    } catch (Exception parseFail) {
      throw new ApiException(
          new ErrorResponse("Ошибка сервера: " + response.body(), response.statusCode()));
    }
  }

  public JwtTokenResponse refresh() throws IOException, InterruptedException {
    refreshInternal();
    return new JwtTokenResponse(accessToken, refreshToken);
  }

  private void refreshInternal() throws IOException, InterruptedException {
    if (refreshToken == null) {
      throw new ApiException(new ErrorResponse("Ошибка: нет refresh токена", 401));
    }

    String jsonBody = objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken));
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v1/auth/refresh"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() >= 400) {
      throw new ApiException(
          new ErrorResponse("Refresh failed: " + response.body(), response.statusCode()));
    }

    JwtTokenResponse tokens = objectMapper.readValue(response.body(), JwtTokenResponse.class);
    this.accessToken = tokens.accessToken();
    this.refreshToken = tokens.refreshToken();
  }
}
