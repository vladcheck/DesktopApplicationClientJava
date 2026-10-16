package mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.DesktopApplicationClientJava.Api.Dto.response.UserDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Epic("Отображение данных")
@Feature("UserDto")
@Story("Разбор контракта пользователя")
@Tag("role:none")
@DisplayName("Разбор пользователя из JSON")
class UserDtoParsingTest {

  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Test
  @DisplayName("Контракт бэкенда разбирается один к одному")
  void parsesBackendContractOneToOne() throws Exception {
    String json =
        """
        {
          "uuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
          "email": "user@example.com",
          "role": "USER",
          "firstname": "Иван",
          "lastname": "Петров",
          "enabled": true,
          "createdAt": "2026-01-02T03:04:05",
          "updatedAt": "2026-01-03T04:05:06"
        }
        """;

    UserDto dto = mapper.readValue(json, UserDto.class);

    assertEquals("Иван", dto.firstname());
    assertEquals("Петров", dto.lastname());
    assertEquals("user@example.com", dto.email());
  }

  @Test
  @DisplayName("Неизвестные поля завершаются ошибкой")
  void unknownPropertiesFailFast() {
    String json = "{\"uuid\": null, \"unexpectedField\": 1}";

    assertThrows(Exception.class, () -> mapper.readValue(json, UserDto.class));
  }
}
