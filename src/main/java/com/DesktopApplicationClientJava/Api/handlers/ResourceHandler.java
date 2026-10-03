package com.DesktopApplicationClientJava.Api.handlers;

import com.DesktopApplicationClientJava.Api.Dto.request.Resource.CreateResourceRequest;
import com.DesktopApplicationClientJava.Api.Dto.request.Resource.ResourceFilterRequest;
import com.DesktopApplicationClientJava.Api.Dto.request.Resource.UpdateResourceRequest;
import com.DesktopApplicationClientJava.Api.Dto.response.ResourceDto;
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
public class ResourceHandler extends Handler {
  public ResourceHandler(HttpController httpController) {
    super(httpController);
  }

  // Работа с Ресурсом(Документом)
  public ResourceDto createResource(CreateResourceRequest request)
      throws IOException, InterruptedException {
    String jsonBody = httpController.objectMapper.writeValueAsString(request);
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(httpController.baseUrl + "/api/v1/resource"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + httpController.accessToken)
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();
    HttpResponse<String> response = httpController.send(httpRequest);
    httpController.checkError(response);
    return httpController.parseResponse(response, ResourceDto.class);
  }

  // Поиск ресурса по Id
  public ResourceDto findResourceById(UUID id) throws IOException, InterruptedException {
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(httpController.baseUrl + "/api/v1/resource/" + id))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + httpController.accessToken)
            .GET()
            .build();
    HttpResponse<String> response = httpController.send(httpRequest);
    httpController.checkError(response);
    return httpController.parseResponse(response, ResourceDto.class);
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
        httpController.baseUrl
            + "/api/v1/resource"
            + (params.isEmpty() ? "" : "?" + String.join("&", params));

    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + httpController.accessToken)
            .GET()
            .build();
    HttpResponse<String> response = httpController.send(httpRequest);
    httpController.checkError(response);

    ResourceDto[] arr = httpController.objectMapper.readValue(response.body(), ResourceDto[].class);
    return Arrays.asList(arr);
  }

  // Обновление ресурса
  public ResourceDto updateResource(UUID uuid, UpdateResourceRequest request)
      throws IOException, InterruptedException {
    String jsonBody = httpController.objectMapper.writeValueAsString(request);
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(httpController.baseUrl + "/api/v1/resource/" + uuid))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + httpController.accessToken)
            .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

    HttpResponse<String> response = httpController.send(httpRequest);
    httpController.checkError(response);
    return httpController.parseResponse(response, ResourceDto.class);
  }

  // Удаление ресурса
  public void deleteResource(UUID uuid) throws IOException, InterruptedException {
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(httpController.baseUrl + "/api/v1/resource/" + uuid))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + httpController.accessToken)
            .DELETE()
            .build();
    HttpResponse<String> response = httpController.send(httpRequest);
    httpController.checkError(response);
  }
}
