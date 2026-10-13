package mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.DesktopApplicationClientJava.Api.Dto.response.FileDto;
import com.DesktopApplicationClientJava.entities.FileInfo;
import com.DesktopApplicationClientJava.mapper.FileMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FileMapperTest {

  @Test
  void mapsAllFields() {
    UUID uuid = UUID.randomUUID();
    FileDto dto = new FileDto(uuid, "отчёт.pdf", "application/pdf", 12345L);

    FileInfo file = FileMapper.INSTANCE.toEntity(dto);

    assertEquals(uuid, file.getUuid());
    assertEquals("отчёт.pdf", file.getName());
    assertEquals("application/pdf", file.getContentType());
    assertEquals(12345L, file.getSize());
  }
}
