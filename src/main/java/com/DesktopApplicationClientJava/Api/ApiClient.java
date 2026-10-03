package com.DesktopApplicationClientJava.Api;

import com.DesktopApplicationClientJava.Api.Dto.error.ErrorResponse;
import com.DesktopApplicationClientJava.Api.Dto.request.Admin.RegisterRequest;
import com.DesktopApplicationClientJava.Api.Dto.request.Admin.UpdateUserRequest;
import com.DesktopApplicationClientJava.Api.Dto.request.Admin.UserFilterRequest;
import com.DesktopApplicationClientJava.Api.Dto.request.Auth.LoginRequest;
import com.DesktopApplicationClientJava.Api.Dto.request.Auth.RefreshTokenRequest;
import com.DesktopApplicationClientJava.Api.Dto.request.Resource.CreateResourceRequest;
import com.DesktopApplicationClientJava.Api.Dto.request.Resource.ResourceFilterRequest;
import com.DesktopApplicationClientJava.Api.Dto.request.Resource.UpdateResourceRequest;
import com.DesktopApplicationClientJava.Api.Dto.response.FileDto;
import com.DesktopApplicationClientJava.Api.Dto.response.Jwt.JwtTokenResponse;
import com.DesktopApplicationClientJava.Api.Dto.response.ResourceDto;
import com.DesktopApplicationClientJava.Api.Dto.response.UserDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ApiClient {
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String baseUrl;

  private String accessToken;
  private String refreshToken;

  public ApiClient(String baseUrl) {
    this.baseUrl = baseUrl;
    this.httpClient = HttpClient.newHttpClient();
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule());
  }

  // Вход в систему
  public JwtTokenResponse login(LoginRequest request) throws IOException, InterruptedException {
    String jsonBody = objectMapper.writeValueAsString(request);
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v1/auth/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();
    HttpResponse<String> response = send(httpRequest);
    checkError(response);
    JwtTokenResponse tokenResponse = parseResponse(response, JwtTokenResponse.class);
    this.accessToken = tokenResponse.accessToken();
    this.refreshToken = tokenResponse.refreshToken();
    return tokenResponse;
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

  // Выход из системы
  public void logout() throws IOException, InterruptedException {
    if (accessToken == null) {
      return;
    }
    String jsonBody = objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken));
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v1/auth/logout"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + accessToken)
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();
    HttpResponse<String> response = send(httpRequest);
    checkError(response);
    this.accessToken = null;
    this.refreshToken = null;
  }

  // Получение информации пользователя о себе
  public UserDto getMe() throws IOException, InterruptedException {
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v1/user/me"))
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();

    HttpResponse<String> response = send(httpRequest);
    checkError(response);
    return parseResponse(response, UserDto.class);
  }

  // Работа с Ресурсом(Документом)
  public ResourceDto createResource(CreateResourceRequest request)
      throws IOException, InterruptedException {
    String jsonBody = objectMapper.writeValueAsString(request);
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v1/resource"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + accessToken)
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();
    HttpResponse<String> response = send(httpRequest);
    checkError(response);
    return parseResponse(response, ResourceDto.class);
  }

  // Поиск ресурса по Id
  public ResourceDto findResourceById(UUID id) throws IOException, InterruptedException {
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v1/resource/" + id))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();
    HttpResponse<String> response = send(httpRequest);
    checkError(response);
    return parseResponse(response, ResourceDto.class);
  }

  // Поиск ресурса по параметрам
  public List<ResourceDto> findResourcesByFilters(
      ResourceFilterRequest filter, Long offset, Long count, String sortBy, String sortDir)
      throws IOException, InterruptedException {
    List<String> params = new ArrayList<>();
    if (filter != null) {
      if (filter.title() != null && !filter.title().isBlank())
        params.add("title=" + encode(filter.title()));
      if (filter.description() != null && !filter.description().isBlank())
        params.add("description=" + encode(filter.description()));
      // createdAt/updatedAt пока не отправляем — при необходимости добавим формат
    }
    if (offset != null) params.add("offset=" + offset);
    if (count != null) params.add("count=" + count);
    if (sortBy != null) params.add("sortBy=" + sortBy);
    if (sortDir != null) params.add("sortDir=" + sortDir);
    String url =
        baseUrl + "/api/v1/resource" + (params.isEmpty() ? "" : "?" + String.join("&", params));

    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();
    HttpResponse<String> response = send(httpRequest);
    checkError(response);

    ResourceDto[] arr = objectMapper.readValue(response.body(), ResourceDto[].class);
    return Arrays.asList(arr);
  }

  // Обновление ресурса
  public ResourceDto updateResource(UUID uuid, UpdateResourceRequest request)
      throws IOException, InterruptedException {
    String jsonBody = objectMapper.writeValueAsString(request);
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v1/resource/" + uuid))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + accessToken)
            .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

    HttpResponse<String> response = send(httpRequest);
    checkError(response);
    return parseResponse(response, ResourceDto.class);
  }

  // Удаление ресурса
  public void deleteResource(UUID uuid) throws IOException, InterruptedException {
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v1/resource/" + uuid))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + accessToken)
            .DELETE()
            .build();
    HttpResponse<String> response = send(httpRequest);
    checkError(response);
  }

  // Создание пользователя
  public UserDto createUser(RegisterRequest request) throws IOException, InterruptedException {
    String jsonBody = objectMapper.writeValueAsString(request);
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v1/admin"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + accessToken)
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();
    HttpResponse<String> response = send(httpRequest);
    checkError(response);
    return parseResponse(response, UserDto.class);
  }

  // Поиск пользователя по ID
  public UserDto getUserById(UUID uuid) throws IOException, InterruptedException {
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v1/admin/" + uuid))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();
    HttpResponse<String> response = send(httpRequest);
    checkError(response);
    return parseResponse(response, UserDto.class);
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
        baseUrl + "/api/v1/admin" + (params.isEmpty() ? "" : "?" + String.join("&", params));

    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();

    HttpResponse<String> response = send(httpRequest);
    checkError(response);
    UserDto[] users = objectMapper.readValue(response.body(), UserDto[].class);
    return Arrays.asList(users);
  }

  // Обновление пользователя
  public UserDto updateUser(UUID uuid, UpdateUserRequest request)
      throws IOException, InterruptedException {
    String jsonBody = objectMapper.writeValueAsString(request);
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v1/admin/" + uuid))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + accessToken)
            .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();
    HttpResponse<String> response = send(httpRequest);
    checkError(response);
    return parseResponse(response, UserDto.class);
  }

  // Удаление пользователя
  public void deleteUser(UUID uuid) throws IOException, InterruptedException {
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v1/admin/" + uuid))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + accessToken)
            .DELETE()
            .build();
    HttpResponse<String> response = send(httpRequest);
    checkError(response);
  }

  public FileDto uploadFile(UUID resourceUuid, Path filePath)
      throws IOException, InterruptedException {
    String boundary = "----JavaPksBoundary" + System.currentTimeMillis();
    byte[] fileBytes = Files.readAllBytes(filePath);
    String filename = filePath.getFileName().toString();
    String contentType =
        Optional.ofNullable(Files.probeContentType(filePath)).orElse("application/octet-stream");

    ByteArrayOutputStream body = new ByteArrayOutputStream();

    body.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
    body.write(
        ("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n")
            .getBytes(StandardCharsets.UTF_8));
    body.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
    body.write(fileBytes);
    body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v1/resource/" + resourceUuid + "/file"))
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .header("Authorization", "Bearer " + accessToken)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
            .build();

    HttpResponse<String> response = send(httpRequest);
    checkError(response);
    return parseResponse(response, FileDto.class);
  }

  public Path downloadFile(UUID resourceUuid, UUID fileUuid, Path destinationDir)
      throws IOException, InterruptedException {

    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v1/resource/" + resourceUuid + "/file/" + fileUuid))
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();

    HttpResponse<InputStream> response =
        executeWithRetry(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

    if (response.statusCode() >= 400) {
      String body = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
      response.body().close();
      try {
        ErrorResponse error = objectMapper.readValue(body, ErrorResponse.class);
        throw new ApiException(error);
      } catch (ApiException e) {
        throw e;
      } catch (Exception parseFail) {
        throw new ApiException(new ErrorResponse("Ошибка сервера: " + body, response.statusCode()));
      }
    }

    String filename =
        parseFilename(response.headers().firstValue("Content-Disposition").orElse(null));
    if (filename == null || filename.isBlank()) {
      filename = fileUuid.toString();
    }

    Files.createDirectories(destinationDir);
    Path target = destinationDir.resolve(filename);

    try (InputStream in = response.body()) {
      Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
    return target;
  }

  public void deleteFile(UUID resourceUuid, UUID fileUuid)
      throws IOException, InterruptedException {
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v1/resource/" + resourceUuid + "/file/" + fileUuid))
            .header("Authorization", "Bearer " + accessToken)
            .DELETE()
            .build();
    HttpResponse<String> response = send(httpRequest);
    checkError(response);
  }

  private static String parseFilename(String cd) {
    if (cd == null) return null;

    int starIdx = cd.indexOf("filename*=");
    if (starIdx >= 0) {
      String val = cd.substring(starIdx + "filename*=".length()).trim();
      int semi = val.indexOf(';');
      if (semi >= 0) val = val.substring(0, semi).trim();
      int encIdx = val.indexOf("''");
      if (encIdx >= 0) {
        String encoded = val.substring(encIdx + 2);
        return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
      }
    }

    int idx = cd.indexOf("filename=");
    if (idx >= 0) {
      String val = cd.substring(idx + "filename=".length()).trim();
      if (val.startsWith("\"")) {
        int end = val.indexOf('"', 1);
        if (end > 0) return val.substring(1, end);
      } else {
        int semi = val.indexOf(';');
        if (semi >= 0) val = val.substring(0, semi);
        return val.trim();
      }
    }
    return null;
  }

  // Выгрузка данных (пользователей, документов и их файлов)
  public byte[] exportUsers() throws IOException, InterruptedException {
    return downloadBinary("/api/v1/admin/users/export");
  }

  public byte[] exportResources() throws IOException, InterruptedException {
    return downloadBinary("/api/v1/admin/resource/export");
  }

  public byte[] exportResourceFiles(UUID resourceId) throws IOException, InterruptedException {
    return downloadBinary("/api/v1/admin/resource/" + resourceId + "/files/export");
  }

  private byte[] downloadBinary(String path) throws IOException, InterruptedException {
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();

    HttpResponse<byte[]> response =
        executeWithRetry(httpRequest, HttpResponse.BodyHandlers.ofByteArray());

    if (response.statusCode() >= 400) {
      String body = new String(response.body(), StandardCharsets.UTF_8);
      try {
        ErrorResponse error = objectMapper.readValue(body, ErrorResponse.class);
        throw new ApiException(error);
      } catch (ApiException e) {
        throw e;
      } catch (Exception parseFail) {
        throw new ApiException(new ErrorResponse("Ошибка сервера: " + body, response.statusCode()));
      }
    }
    return response.body();
  }

  private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
    return executeWithRetry(request, HttpResponse.BodyHandlers.ofString());
  }

  // Проверка ошибок сервера
  private void checkError(HttpResponse<String> response) {
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

  // Парсер ответа
  private <T> T parseResponse(HttpResponse<String> response, Class<T> clazz) throws IOException {
    if (response.body() == null || response.body().isBlank()) {
      return null;
    }
    return objectMapper.readValue(response.body(), clazz);
  }

  private <T> HttpResponse<T> executeWithRetry(
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

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  public String getAccessToken() {
    return accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public HttpClient getHttpClient() {
    return httpClient;
  }

  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  public String getBaseUrl() {
    return baseUrl;
  }
}
