package mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.DesktopApplicationClientJava.Api.Dto.response.UserDto;
import com.DesktopApplicationClientJava.entities.Role;
import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserMapperTest {

  @Test
  void mapsBackendLowercaseNamesToCamelCaseFields() {
    UUID uuid = UUID.randomUUID();
    UserDto dto =
        new UserDto(
            uuid,
            "user@example.com",
            Role.USER,
            "Иван",
            "Петров",
            true,
            LocalDateTime.parse("2026-01-02T03:04:05"),
            LocalDateTime.parse("2026-01-03T04:05:06"));

    User user = UserMapper.toEntity(dto);

    assertEquals(uuid, user.getUuid());
    assertEquals("user@example.com", user.getEmail());
    assertEquals(Role.USER, user.getRole());
    assertEquals("Иван", user.getFirstName());
    assertEquals("Петров", user.getLastName());
    assertEquals(true, user.getEnabled());
  }
}
