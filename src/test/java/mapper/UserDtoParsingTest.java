package mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.DesktopApplicationClientJava.Api.Dto.response.UserDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

class UserDtoParsingTest {

  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Test
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
  void unknownPropertiesFailFast() {
    String json = "{\"uuid\": null, \"unexpectedField\": 1}";

    assertThrows(Exception.class, () -> mapper.readValue(json, UserDto.class));
  }
}
