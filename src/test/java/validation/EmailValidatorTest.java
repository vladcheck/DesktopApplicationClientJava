package validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.DesktopApplicationClientJava.validation.EmailValidator;
import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.StringField;
import com.dlsc.formsfx.model.validators.Validator;
import java.util.List;
import org.junit.jupiter.api.Test;

public class EmailValidatorTest {
  private static final String FIELD_NAME = EmailValidator.FIELD_NAME;

  private static StringField fieldWith(String value) {
    List<Validator<String>> validators = EmailValidator.validators();
    @SuppressWarnings("unchecked")
    Validator<String>[] array = validators.toArray(new Validator[0]);
    return Field.ofStringType(value).required(ErrorMessages.Required).validate(array);
  }

  @Test
  void emptyEmailIsRejected() {
    assertEquals(ErrorMessages.Required, EmailValidator.validate(""));
  }

  @Test
  void blankEmailIsRejected() {
    assertEquals(ErrorMessages.Required, EmailValidator.validate("   "));
  }

  @Test
  void shortEmailIsRejected() {
    assertEquals(
        ErrorMessages.MinLength(FIELD_NAME, EmailValidator.MIN_EMAIL_LENGTH),
        EmailValidator.validate("a@b"));
  }

  @Test
  void longEmailIsRejected() {
    String e = "a".repeat(95) + "@x.com";
    assertEquals(
        ErrorMessages.MaxLength(FIELD_NAME, EmailValidator.MAX_EMAIL_LENGTH),
        EmailValidator.validate(e));
  }

  @Test
  void emailWithoutAtIsRejected() {
    assertEquals(ErrorMessages.IncorrectFormat, EmailValidator.validate("nope.com"));
  }

  @Test
  void validEmailIsAccepted() {
    assertNull(EmailValidator.validate("test@example.com"));
  }

  @Test
  void fieldRejectsEmptyEmail() {
    StringField field = fieldWith("");

    assertFalse(field.isValid());
    assertTrue(field.getErrorMessages().contains(ErrorMessages.Required));
  }

  @Test
  void fieldRejectsInvalidEmail() {
    StringField field = fieldWith("nope.com");

    assertFalse(field.isValid());
    assertTrue(field.getErrorMessages().contains(ErrorMessages.IncorrectFormat));
  }

  @Test
  void fieldAcceptsValidEmail() {
    assertTrue(fieldWith("test@example.com").isValid());
  }
}
