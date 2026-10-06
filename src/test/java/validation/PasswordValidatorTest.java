package validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.DesktopApplicationClientJava.validation.PasswordValidator;
import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import javafx.beans.property.SimpleStringProperty;
import net.synedra.validatorfx.Validator;
import org.junit.jupiter.api.Test;

public class PasswordValidatorTest extends ValidatorFxTest {
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

  @Test
  void checkRejectsShortPassword() {
    Validator validator = new Validator();
    SimpleStringProperty password = new SimpleStringProperty("short");
    PasswordValidator.createCheck(validator, password);

    assertFalse(validator.validate());
    assertTrue(validator.containsErrors());
    assertEquals(
        ErrorMessages.MinLength(FIELD_NAME, PasswordValidator.MIN_LENGTH),
        validator.getValidationResult().getMessages().get(0).getText());
  }

  @Test
  void checkAcceptsValidPassword() {
    Validator validator = new Validator();
    SimpleStringProperty password = new SimpleStringProperty("12345678");
    PasswordValidator.createCheck(validator, password);

    assertTrue(validator.validate());
  }
}
