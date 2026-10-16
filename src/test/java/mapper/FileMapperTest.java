package mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.DesktopApplicationClientJava.Api.Dto.response.FileDto;
import com.DesktopApplicationClientJava.entities.FileInfo;
import com.DesktopApplicationClientJava.mapper.FileMapper;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Epic("Отображение данных")
@Feature("FileMapper")
@Story("Преобразование файла в сущность")
@Tag("role:none")
@DisplayName("Преобразование файла")
class FileMapperTest {

  @Test
  @DisplayName("Все поля файла преобразуются")
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
