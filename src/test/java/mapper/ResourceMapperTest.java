package mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.DesktopApplicationClientJava.Api.Dto.response.FileDto;
import com.DesktopApplicationClientJava.Api.Dto.response.ResourceDto;
import com.DesktopApplicationClientJava.entities.Resource;
import com.DesktopApplicationClientJava.mapper.ResourceMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResourceMapperTest {

  @Test
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

    Resource resource = ResourceMapper.toEntity(dto);

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
  void nullFilesBecomeEmptyList() {
    ResourceDto dto =
        new ResourceDto(
            UUID.randomUUID(), "Отчёт", "Описание", LocalDateTime.now(), LocalDateTime.now(), null);

    Resource resource = ResourceMapper.toEntity(dto);

    assertTrue(resource.getFiles().isEmpty());
  }
}
