package validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.DesktopApplicationClientJava.validation.ThirdNameValidator;
import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import org.junit.jupiter.api.Test;

public class ThirdNameValidatorTest {
  private static final String FIELD_NAME = ThirdNameValidator.FIELD_NAME;

  @Test
  void emptyThirdNameIsAccepted() {
    assertNull(ThirdNameValidator.validate(""));
  }

  @Test
  void blankThirdNameIsRejected() {
    assertEquals(ErrorMessages.IncorrectFormat, ThirdNameValidator.validate("   "));
  }

  @Test
  void shortThirdNameIsRejected() {
    assertEquals(
        ErrorMessages.MinLength(FIELD_NAME, ThirdNameValidator.MIN_LENGTH),
        ThirdNameValidator.validate("А"));
  }

  @Test
  void validThirdNameIsAccepted() {
    assertNull(ThirdNameValidator.validate("Антон"));
  }
}
