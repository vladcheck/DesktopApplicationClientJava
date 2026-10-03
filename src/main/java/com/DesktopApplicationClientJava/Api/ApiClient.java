package com.DesktopApplicationClientJava.Api;

import com.DesktopApplicationClientJava.Api.Dto.response.UserDto;
import com.DesktopApplicationClientJava.Api.handlers.AuthHandler;
import com.DesktopApplicationClientJava.Api.handlers.ExportHandler;
import com.DesktopApplicationClientJava.Api.handlers.FileHandler;
import com.DesktopApplicationClientJava.Api.handlers.ResourceHandler;
import com.DesktopApplicationClientJava.Api.handlers.UserHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.Getter;

@Getter
public class ApiClient {
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String baseUrl;

  private String accessToken;
  private String refreshToken;

  private final HttpController httpController;
  private final AuthHandler authHandler;
  private final UserHandler userHandler;
  private final ResourceHandler resourceHandler;
  private final FileHandler fileHandler;
  private final ExportHandler exportHandler;

  public ApiClient(String baseUrl) {
    this.baseUrl = baseUrl;
    this.httpClient = HttpClient.newHttpClient();
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule());

    this.httpController = new HttpController(baseUrl, httpClient, objectMapper, refreshToken, accessToken);
    this.authHandler = new AuthHandler(httpController);
    this.userHandler = new UserHandler(httpController);
    this.resourceHandler = new ResourceHandler(httpController);
    this.fileHandler = new FileHandler(httpController);
    this.exportHandler = new ExportHandler(httpController);
  }

  // Получение информации пользователя о себе
  public UserDto getMe() throws IOException, InterruptedException {
    HttpRequest httpRequest = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/api/v1/user/me"))
        .header("Authorization", "Bearer " + accessToken)
        .GET()
        .build();

    HttpResponse<String> response = httpController.send(httpRequest);
    httpController.checkError(response);
    return httpController.parseResponse(response, UserDto.class);
  }
}
