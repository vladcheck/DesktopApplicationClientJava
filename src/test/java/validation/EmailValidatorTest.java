package validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.DesktopApplicationClientJava.validation.EmailValidator;
import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import javafx.beans.property.SimpleStringProperty;
import net.synedra.validatorfx.Validator;
import org.junit.jupiter.api.Test;

public class EmailValidatorTest extends ValidatorFxTest {
  private static final String FIELD_NAME = EmailValidator.FIELD_NAME;

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
  void checkRejectsInvalidEmail() {
    Validator validator = new Validator();
    SimpleStringProperty email = new SimpleStringProperty("nope.com");
    EmailValidator.createCheck(validator, email);

    assertFalse(validator.validate());
    assertTrue(validator.containsErrors());
    assertEquals(1, validator.getValidationResult().getMessages().size());
    assertEquals(
        ErrorMessages.IncorrectFormat,
        validator.getValidationResult().getMessages().get(0).getText());
  }

  @Test
  void checkAcceptsValidEmail() {
    Validator validator = new Validator();
    SimpleStringProperty email = new SimpleStringProperty("test@example.com");
    EmailValidator.createCheck(validator, email);

    assertTrue(validator.validate());
  }
}
