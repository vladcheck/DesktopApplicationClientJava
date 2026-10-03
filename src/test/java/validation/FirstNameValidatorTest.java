package validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.DesktopApplicationClientJava.validation.FirstNameValidator;
import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import org.junit.jupiter.api.Test;

public class FirstNameValidatorTest {
  private static final String FIELD_NAME = FirstNameValidator.FIELD_NAME;
  private static final FirstNameValidator firstNameValidator = new FirstNameValidator();

  @Test
  void emptyFirstNameIsRejected() {
    assertEquals(ErrorMessages.Required, firstNameValidator.isValid(""));
  }

  @Test
  void validFirstNameIsAccepted() {
    assertNull(firstNameValidator.isValid("Антон"));
  }
}
