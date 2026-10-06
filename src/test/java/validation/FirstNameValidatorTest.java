package validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.DesktopApplicationClientJava.validation.FirstNameValidator;
import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import org.junit.jupiter.api.Test;

public class FirstNameValidatorTest {
  private static final String FIELD_NAME = FirstNameValidator.FIELD_NAME;

  @Test
  void emptyFirstNameIsRejected() {
    assertEquals(ErrorMessages.Required, FirstNameValidator.validate(""));
  }

  @Test
  void shortFirstNameIsRejected() {
    assertEquals(
        ErrorMessages.MinLength(FIELD_NAME, FirstNameValidator.MIN_LENGTH),
        FirstNameValidator.validate("А"));
  }

  @Test
  void validFirstNameIsAccepted() {
    assertNull(FirstNameValidator.validate("Антон"));
  }
}
