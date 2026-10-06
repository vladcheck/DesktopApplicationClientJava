package com.DesktopApplicationClientJava.services.mock;

import com.DesktopApplicationClientJava.entities.FileInfo;
import com.DesktopApplicationClientJava.entities.Resource;
import com.DesktopApplicationClientJava.entities.Role;
import com.DesktopApplicationClientJava.entities.User;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;

/**
 * In-memory data for mock services. Used only when the app runs with {@code MOCK_DATA=true}, for UI
 * testing without a backend.
 */
@Getter
public class MockStore {
  private final Map<UUID, User> users = new LinkedHashMap<>();
  private final Map<UUID, Resource> resources = new LinkedHashMap<>();
  private final Map<UUID, byte[]> fileBytes = new LinkedHashMap<>();

  public MockStore() {
    seedUsers();
    seedResources();
  }

  private void seedUsers() {
    users.put(
        UUID.fromString("11111111-1111-1111-1111-111111111111"),
        new User(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "admin@example.com",
            Role.ADMIN,
            "Админ",
            "Моковый",
            true));
    users.put(
        UUID.fromString("22222222-2222-2222-2222-222222222222"),
        new User(
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            "moder@example.com",
            Role.MODER,
            "Модер",
            "Моковый",
            true));
    users.put(
        UUID.fromString("33333333-3333-3333-3333-333333333333"),
        new User(
            UUID.fromString("33333333-3333-3333-3333-333333333333"),
            "user@example.com",
            Role.USER,
            "Юзер",
            "Моковый",
            true));
  }

  private void seedResources() {
    UUID resourceId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    FileInfo report =
        new FileInfo(
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
            "report.pdf",
            "application/pdf",
            25L);
    FileInfo data =
        new FileInfo(
            UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"), "data.csv", "text/csv", 18L);
    fileBytes.put(
        report.getUuid(), "mock pdf content, not a real pdf".getBytes(StandardCharsets.UTF_8));
    fileBytes.put(data.getUuid(), "id;name\n1;mock\n".getBytes(StandardCharsets.UTF_8));
    resources.put(
        resourceId,
        new Resource(
            resourceId,
            "Годовой отчёт (мок)",
            "Демонстрационный ресурс для проверки UI без бэкенда",
            LocalDateTime.of(2024, 5, 6, 7, 8),
            LocalDateTime.of(2024, 5, 7, 9, 10),
            new ArrayList<>(List.of(report, data))));

    UUID emptyId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    resources.put(
        emptyId,
        new Resource(
            emptyId,
            "Пустой ресурс (мок)",
            "Ресурс без файлов",
            LocalDateTime.of(2024, 6, 1, 12, 0),
            LocalDateTime.of(2024, 6, 1, 12, 0),
            new ArrayList<>()));
  }
}
