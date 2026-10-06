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
import com.DesktopApplicationClientJava.session.Session;
import com.fasterxml.jackson.databind.DeserializationFeature;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ApiClient {

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final Session session;

  public ApiClient(Session session) {
    this.session = session;
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(ApiConfig.connectTimeoutMs()))
            .build();
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule());
    this.objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  // ======================== AUTH ========================

  public JwtTokenResponse login(LoginRequest request) throws IOException, InterruptedException {
    String json = objectMapper.writeValueAsString(request);
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(uri(ApiConfig.pathLogin()))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
    HttpResponse<String> response = send(httpRequest);
    checkError(response);
    JwtTokenResponse tokens = parseResponse(response, JwtTokenResponse.class);
    session.setAccessToken(tokens.accessToken());
    session.setRefreshToken(tokens.refreshToken());
    return tokens;
  }

  public JwtTokenResponse refresh() throws IOException, InterruptedException {
    refreshInternal();
    return new JwtTokenResponse(session.getAccessToken(), session.getRefreshToken());
  }

  private void refreshInternal() throws IOException, InterruptedException {
    String currentRefresh = session.getRefreshToken();
    if (currentRefresh == null) {
      throw new ApiException(new ErrorResponse("Ошибка: нет refresh токена", 401));
    }

    String json = objectMapper.writeValueAsString(new RefreshTokenRequest(currentRefresh));
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(uri(ApiConfig.pathRefresh()))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() >= 400) {
      throw new ApiException(
          new ErrorResponse("Refresh failed: " + response.body(), response.statusCode()));
    }

    JwtTokenResponse tokens = objectMapper.readValue(response.body(), JwtTokenResponse.class);
    session.setAccessToken(tokens.accessToken());
    session.setRefreshToken(tokens.refreshToken());
  }

  public void logout() throws IOException, InterruptedException {
    String access = session.getAccessToken();
    if (access == null) return;

    String json =
        objectMapper.writeValueAsString(new RefreshTokenRequest(session.getRefreshToken()));
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(uri(ApiConfig.pathLogout()))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + access)
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
    HttpResponse<String> response = send(httpRequest);
    checkError(response);
    session.clear();
  }

  public UserDto getMe() throws IOException, InterruptedException {
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(uri(ApiConfig.pathMe()))
            .header("Authorization", "Bearer " + session.getAccessToken())
            .GET()
            .build();
    HttpResponse<String> response = send(httpRequest);
    checkError(response);
    return parseResponse(response, UserDto.class);
  }

  // ======================== RESOURCE ========================

  public ResourceDto createResource(CreateResourceRequest request)
      throws IOException, InterruptedException {
    String json = objectMapper.writeValueAsString(request);
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(uri(ApiConfig.pathResource()))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + session.getAccessToken())
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
    HttpResponse<String> response = send(httpRequest);
    checkError(response);
    return parseResponse(response, ResourceDto.class);
  }

  public ResourceDto findResourceById(UUID id) throws IOException, InterruptedException {
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(uri(ApiConfig.pathResource() + "/" + id))
            .header("Authorization", "Bearer " + session.getAccessToken())
            .GET()
            .build();
    HttpResponse<String> response = send(httpRequest);
    checkError(response);
    return parseResponse(response, ResourceDto.class);
  }

  public List<ResourceDto> findResourcesByFilters(
      ResourceFilterRequest filter, Long offset, Long count, String sortBy, String sortDir)
      throws IOException, InterruptedException {
    List<String> params = new ArrayList<>();
    if (filter != null) {
      if (filter.title() != null && !filter.title().isBlank())
        params.add("title=" + encode(filter.title()));
      if (filter.description() != null && !filter.description().isBlank())
        params.add("description=" + encode(filter.description()));
    }
    if (offset != null) params.add("offset=" + offset);
    if (count != null) params.add("count=" + count);
    if (sortBy != null && !sortBy.isBlank()) params.add("sortBy=" + encode(sortBy));
    if (sortDir != null && !sortDir.isBlank()) params.add("sortDir=" + encode(sortDir));

    String url =
        ApiConfig.baseUrl()
            + ApiConfig.pathResource()
            + (params.isEmpty() ? "" : "?" + String.join("&", params));

    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + session.getAccessToken())
            .GET()
            .build();
    HttpResponse<String> response = send(httpRequest);
    checkError(response);
    ResourceDto[] arr = objectMapper.readValue(response.body(), ResourceDto[].class);
    return Arrays.asList(arr);
  }

  public ResourceDto updateResource(UUID uuid, UpdateResourceRequest request)
      throws IOException, InterruptedException {
    String json = objectMapper.writeValueAsString(request);
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(uri(ApiConfig.pathResource() + "/" + uuid))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + session.getAccessToken())
            .PUT(HttpRequest.BodyPublishers.ofString(json))
            .build();
    HttpResponse<String> response = send(httpRequest);
    checkError(response);
    return parseResponse(response, ResourceDto.class);
  }

  public void deleteResource(UUID uuid) throws IOException, InterruptedException {
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(uri(ApiConfig.pathResource() + "/" + uuid))
            .header("Authorization", "Bearer " + session.getAccessToken())
            .DELETE()
            .build();
    HttpResponse<String> response = send(httpRequest);
    checkError(response);
  }

  // ======================== ADMIN ========================

  public UserDto createUser(RegisterRequest request) throws IOException, InterruptedException {
    String json = objectMapper.writeValueAsString(request);
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(uri(ApiConfig.pathAdmin()))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + session.getAccessToken())
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
    HttpResponse<String> response = send(httpRequest);
    checkError(response);
    return parseResponse(response, UserDto.class);
  }

  public UserDto getUserById(UUID uuid) throws IOException, InterruptedException {
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(uri(ApiConfig.pathAdmin() + "/" + uuid))
            .header("Authorization", "Bearer " + session.getAccessToken())
            .GET()
            .build();
    HttpResponse<String> response = send(httpRequest);
    checkError(response);
    return parseResponse(response, UserDto.class);
  }

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
    if (sortBy != null && !sortBy.isBlank()) params.add("sortBy=" + encode(sortBy));
    if (sortDir != null && !sortDir.isBlank()) params.add("sortDir=" + encode(sortDir));

    String url =
        ApiConfig.baseUrl()
            + ApiConfig.pathAdmin()
            + (params.isEmpty() ? "" : "?" + String.join("&", params));

    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + session.getAccessToken())
            .GET()
            .build();
    HttpResponse<String> response = send(httpRequest);
    checkError(response);
    UserDto[] users = objectMapper.readValue(response.body(), UserDto[].class);
    return Arrays.asList(users);
  }

  public UserDto updateUser(UUID uuid, UpdateUserRequest request)
      throws IOException, InterruptedException {
    String json = objectMapper.writeValueAsString(request);
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(uri(ApiConfig.pathAdmin() + "/" + uuid))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + session.getAccessToken())
            .PUT(HttpRequest.BodyPublishers.ofString(json))
            .build();
    HttpResponse<String> response = send(httpRequest);
    checkError(response);
    return parseResponse(response, UserDto.class);
  }

  public void deleteUser(UUID uuid) throws IOException, InterruptedException {
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(uri(ApiConfig.pathAdmin() + "/" + uuid))
            .header("Authorization", "Bearer " + session.getAccessToken())
            .DELETE()
            .build();
    HttpResponse<String> response = send(httpRequest);
    checkError(response);
  }

  // ======================== FILES ========================

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
            .uri(uri(ApiConfig.pathResourceFiles(resourceUuid.toString())))
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .header("Authorization", "Bearer " + session.getAccessToken())
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
            .uri(uri(ApiConfig.pathResourceFile(resourceUuid.toString(), fileUuid.toString())))
            .header("Authorization", "Bearer " + session.getAccessToken())
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
            .uri(uri(ApiConfig.pathResourceFile(resourceUuid.toString(), fileUuid.toString())))
            .header("Authorization", "Bearer " + session.getAccessToken())
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

  // ======================== EXPORT ========================

  public byte[] exportUsers() throws IOException, InterruptedException {
    return downloadBinary(ApiConfig.pathExportUsers());
  }

  public byte[] exportResources() throws IOException, InterruptedException {
    return downloadBinary(ApiConfig.pathExportResources());
  }

  public byte[] exportResourceFiles(UUID resourceId) throws IOException, InterruptedException {
    return downloadBinary(ApiConfig.pathExportResourceFiles(resourceId.toString()));
  }

  private byte[] downloadBinary(String path) throws IOException, InterruptedException {
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(uri(path))
            .header("Authorization", "Bearer " + session.getAccessToken())
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

  // ======================== HELPERS ========================

  private URI uri(String path) {
    return URI.create(ApiConfig.baseUrl() + path);
  }

  private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
    return executeWithRetry(request, HttpResponse.BodyHandlers.ofString());
  }

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

  private <T> T parseResponse(HttpResponse<String> response, Class<T> clazz) throws IOException {
    if (response.body() == null || response.body().isBlank()) return null;
    return objectMapper.readValue(response.body(), clazz);
  }

  private <T> HttpResponse<T> executeWithRetry(
      HttpRequest request, HttpResponse.BodyHandler<T> handler)
      throws IOException, InterruptedException {

    HttpResponse<T> response = httpClient.send(request, handler);
    if (response.statusCode() != 401) return response;
    if (request.uri().getPath().startsWith("/api/v1/auth/")) return response;
    if (session.getRefreshToken() == null) return response;

    try {
      refreshInternal();
    } catch (Exception e) {
      return response;
    }

    HttpRequest retried =
        HttpRequest.newBuilder(request, (name, value) -> !name.equalsIgnoreCase("Authorization"))
            .header("Authorization", "Bearer " + session.getAccessToken())
            .build();
    return httpClient.send(retried, handler);
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
