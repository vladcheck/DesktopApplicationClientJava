package mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.DesktopApplicationClientJava.Api.Dto.response.FileDto;
import com.DesktopApplicationClientJava.Api.Dto.response.ResourceDto;
import com.DesktopApplicationClientJava.entities.Resource;
import com.DesktopApplicationClientJava.mapper.ResourceMapper;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Epic("Отображение данных")
@Feature("ResourceMapper")
@Story("Преобразование ресурса в сущность")
@Tag("role:none")
@DisplayName("Преобразование ресурса")
class ResourceMapperTest {

  @Test
  @DisplayName("Все поля ресурса с файлами преобразуются")
  void mapsAllFieldsIncludingFiles() {
    UUID uuid = UUID.randomUUID();
    UUID fileUuid = UUID.randomUUID();
    ResourceDto dto =
        new ResourceDto(
            uuid,
            "Отчёт",
            "Описание",
            LocalDateTime.parse("2026-01-02T03:04:05"),
            LocalDateTime.parse("2026-01-03T04:05:06"),
            List.of(new FileDto(fileUuid, "отчёт.pdf", "application/pdf", 12345L)));

    Resource resource = ResourceMapper.INSTANCE.toEntity(dto);

    assertEquals(uuid, resource.getUuid());
    assertEquals("Отчёт", resource.getTitle());
    assertEquals("Описание", resource.getDescription());
    assertEquals(LocalDateTime.parse("2026-01-02T03:04:05"), resource.getCreatedAt());
    assertEquals(LocalDateTime.parse("2026-01-03T04:05:06"), resource.getUpdatedAt());
    assertEquals(1, resource.getFiles().size());
    assertEquals(fileUuid, resource.getFiles().get(0).getUuid());
    assertEquals("отчёт.pdf", resource.getFiles().get(0).getName());
    assertEquals("application/pdf", resource.getFiles().get(0).getContentType());
    assertEquals(12345L, resource.getFiles().get(0).getSize());
  }

  @Test
  @DisplayName("Отсутствие файлов даёт пустой список")
  void nullFilesBecomeEmptyList() {
    ResourceDto dto =
        new ResourceDto(
            UUID.randomUUID(), "Отчёт", "Описание", LocalDateTime.now(), LocalDateTime.now(), null);

    Resource resource = ResourceMapper.INSTANCE.toEntity(dto);

    assertTrue(resource.getFiles().isEmpty());
  }
}
