package com.DesktopApplicationClientJava.Api.handlers;

import com.DesktopApplicationClientJava.Api.ApiException;
import com.DesktopApplicationClientJava.Api.Dto.error.ErrorResponse;
import com.DesktopApplicationClientJava.Api.HttpController;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.Setter;

@Setter
public class ExportHandler extends Handler {
  public ExportHandler(HttpController httpController) {
    super(httpController);
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
            .uri(URI.create(httpController.baseUrl + path))
            .header("Authorization", "Bearer " + httpController.accessToken)
            .GET()
            .build();

    HttpResponse<byte[]> response =
        httpController.executeWithRetry(httpRequest, HttpResponse.BodyHandlers.ofByteArray());

    if (response.statusCode() >= 400) {
      String body = new String(response.body(), StandardCharsets.UTF_8);
      try {
        ErrorResponse error = httpController.objectMapper.readValue(body, ErrorResponse.class);
        throw new ApiException(error);
      } catch (ApiException e) {
        throw e;
      } catch (Exception parseFail) {
        throw new ApiException(new ErrorResponse("Ошибка сервера: " + body, response.statusCode()));
      }
    }
    return response.body();
  }
}
