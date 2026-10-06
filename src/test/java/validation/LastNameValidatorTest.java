package validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.DesktopApplicationClientJava.validation.LastNameValidator;
import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import org.junit.jupiter.api.Test;

public class LastNameValidatorTest {
  private static final String FIELD_NAME = LastNameValidator.FIELD_NAME;

  @Test
  void emptyLastNameIsRejected() {
    assertEquals(ErrorMessages.Required, LastNameValidator.validate(""));
  }

  @Test
  void shortLastNameIsRejected() {
    assertEquals(
        ErrorMessages.MinLength(FIELD_NAME, LastNameValidator.MIN_LENGTH),
        LastNameValidator.validate("А"));
  }

  @Test
  void validLastNameIsAccepted() {
    assertNull(LastNameValidator.validate("Антонов"));
  }
}
