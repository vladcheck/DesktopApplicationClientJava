package com.DesktopApplicationClientJava.Api.handlers;

import com.DesktopApplicationClientJava.Api.ApiException;
import com.DesktopApplicationClientJava.Api.Dto.error.ErrorResponse;
import com.DesktopApplicationClientJava.Api.Dto.response.FileDto;
import com.DesktopApplicationClientJava.Api.HttpController;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import lombok.Setter;

@Setter
public class FileHandler extends Handler {
  public FileHandler(HttpController httpController) {
    super(httpController);
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
            .uri(URI.create(httpController.baseUrl + "/api/v1/resource/" + resourceUuid + "/file"))
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .header("Authorization", "Bearer " + httpController.accessToken)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
            .build();

    HttpResponse<String> response = httpController.send(httpRequest);
    httpController.checkError(response);
    return httpController.parseResponse(response, FileDto.class);
  }

  public Path downloadFile(UUID resourceUuid, UUID fileUuid, Path destinationDir)
      throws IOException, InterruptedException {

    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    httpController.baseUrl
                        + "/api/v1/resource/"
                        + resourceUuid
                        + "/file/"
                        + fileUuid))
            .header("Authorization", "Bearer " + httpController.accessToken)
            .GET()
            .build();

    HttpResponse<InputStream> response =
        httpController.executeWithRetry(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

    if (response.statusCode() >= 400) {
      String body = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
      response.body().close();
      try {
        ErrorResponse error = httpController.objectMapper.readValue(body, ErrorResponse.class);
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
            .uri(
                URI.create(
                    httpController.baseUrl
                        + "/api/v1/resource/"
                        + resourceUuid
                        + "/file/"
                        + fileUuid))
            .header("Authorization", "Bearer " + httpController.accessToken)
            .DELETE()
            .build();
    HttpResponse<String> response = httpController.send(httpRequest);
    httpController.checkError(response);
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
}
