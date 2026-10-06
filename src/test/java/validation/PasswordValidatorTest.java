package validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.DesktopApplicationClientJava.validation.PasswordValidator;
import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.PasswordField;
import com.dlsc.formsfx.model.validators.Validator;
import java.util.List;
import org.junit.jupiter.api.Test;

public class PasswordValidatorTest {
  private static final String FIELD_NAME = PasswordValidator.FIELD_NAME;

  private static PasswordField fieldWith(String value) {
    List<Validator<String>> validators = PasswordValidator.validators();
    @SuppressWarnings("unchecked")
    Validator<String>[] array = validators.toArray(new Validator[0]);
    return Field.ofPasswordType(value).required(ErrorMessages.Required).validate(array);
  }

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

  @Test
  void fieldRejectsEmptyPassword() {
    PasswordField field = fieldWith("");

    assertFalse(field.isValid());
    assertTrue(field.getErrorMessages().contains(ErrorMessages.Required));
  }

  @Test
  void fieldRejectsShortPassword() {
    PasswordField field = fieldWith("short");

    assertFalse(field.isValid());
    assertTrue(
        field
            .getErrorMessages()
            .contains(ErrorMessages.MinLength(FIELD_NAME, PasswordValidator.MIN_LENGTH)));
  }

  @Test
  void fieldAcceptsValidPassword() {
    assertTrue(fieldWith("12345678").isValid());
  }
}
