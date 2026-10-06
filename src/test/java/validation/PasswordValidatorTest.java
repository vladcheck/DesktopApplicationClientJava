package validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.DesktopApplicationClientJava.validation.PasswordValidator;
import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import org.junit.jupiter.api.Test;

public class PasswordValidatorTest {
  private static final String FIELD_NAME = PasswordValidator.FIELD_NAME;

  @Test
  void emptyPasswordIsRejected() {
    assertEquals(ErrorMessages.Required, PasswordValidator.validate(""));
  }

  @Test
  void shortPasswordIsRejected() {
    assertEquals(
        ErrorMessages.MinLength(FIELD_NAME, PasswordValidator.MIN_LENGTH),
        PasswordValidator.validate("short"));
  }

  @Test
  void exactlyEightCharPasswordIsAccepted() {
    assertNull(PasswordValidator.validate("12345678"));
  }
}
