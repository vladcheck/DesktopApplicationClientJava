package com.DesktopApplicationClientJava.Api.handlers;

import com.DesktopApplicationClientJava.Api.Dto.request.Admin.RegisterRequest;
import com.DesktopApplicationClientJava.Api.Dto.request.Admin.UpdateUserRequest;
import com.DesktopApplicationClientJava.Api.Dto.request.Admin.UserFilterRequest;
import com.DesktopApplicationClientJava.Api.Dto.response.UserDto;
import com.DesktopApplicationClientJava.Api.HttpController;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.Setter;

@Setter
public class UserHandler extends Handler {
  public UserHandler(HttpController httpController) {
    super(httpController);
  }

  // Создание пользователя
  public UserDto createUser(RegisterRequest request) throws IOException, InterruptedException {
    String jsonBody = httpController.objectMapper.writeValueAsString(request);
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(httpController.baseUrl + "/api/v1/admin"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + httpController.accessToken)
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();
    HttpResponse<String> response = httpController.send(httpRequest);
    httpController.checkError(response);
    return httpController.parseResponse(response, UserDto.class);
  }

  // Поиск пользователя по ID
  public UserDto getUserById(UUID uuid) throws IOException, InterruptedException {
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(httpController.baseUrl + "/api/v1/admin/" + uuid))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + httpController.accessToken)
            .GET()
            .build();
    HttpResponse<String> response = httpController.send(httpRequest);
    httpController.checkError(response);
    return httpController.parseResponse(response, UserDto.class);
  }

  // Поиск пользователя по параметрам
  public List<UserDto> getUsersByFilters(
      UserFilterRequest filter, Long offset, Long count, String sortBy, String sortDir)
      throws IOException, InterruptedException {
    List<String> params = new ArrayList<>();
    if (filter != null) {
      if (filter.email() != null && !filter.email().isBlank())
        params.add("email=" + encode(filter.email()));
      if (filter.firstName() != null && !filter.firstName().isBlank())
        params.add("firstName=" + encode(filter.firstName()));
      if (filter.lastName() != null && !filter.lastName().isBlank())
        params.add("lastName=" + encode(filter.lastName()));
      if (filter.role() != null) params.add("role=" + encode(filter.role().name()));
      if (filter.enabled() != null) params.add("enabled=" + filter.enabled());
    }

    if (offset != null) params.add("offset=" + offset);
    if (count != null) params.add("count=" + count);
    if (sortBy != null) params.add("sortBy=" + encode(sortBy));
    if (sortDir != null) params.add("sortDir=" + encode(sortDir));

    String url =
        httpController.baseUrl
            + "/api/v1/admin"
            + (params.isEmpty() ? "" : "?" + String.join("&", params));

    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + httpController.accessToken)
            .GET()
            .build();

    HttpResponse<String> response = httpController.send(httpRequest);
    httpController.checkError(response);
    UserDto[] users = httpController.objectMapper.readValue(response.body(), UserDto[].class);
    return Arrays.asList(users);
  }

  // Обновление пользователя
  public UserDto updateUser(UUID uuid, UpdateUserRequest request)
      throws IOException, InterruptedException {
    String jsonBody = httpController.objectMapper.writeValueAsString(request);
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(httpController.baseUrl + "/api/v1/admin/" + uuid))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + httpController.accessToken)
            .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();
    HttpResponse<String> response = httpController.send(httpRequest);
    httpController.checkError(response);
    return httpController.parseResponse(response, UserDto.class);
  }

  // Удаление пользователя
  public void deleteUser(UUID uuid) throws IOException, InterruptedException {
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(httpController.baseUrl + "/api/v1/admin/" + uuid))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + httpController.accessToken)
            .DELETE()
            .build();
    HttpResponse<String> response = httpController.send(httpRequest);
    httpController.checkError(response);
  }
}
