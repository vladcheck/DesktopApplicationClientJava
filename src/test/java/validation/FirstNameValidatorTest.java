package validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.DesktopApplicationClientJava.validation.FirstNameValidator;
import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import javafx.beans.property.SimpleStringProperty;
import net.synedra.validatorfx.Validator;
import org.junit.jupiter.api.Test;

public class FirstNameValidatorTest extends ValidatorFxTest {

  @Test
  void emptyFirstNameIsRejected() {
    assertEquals(ErrorMessages.Required, FirstNameValidator.validate(""));
  }

  @Test
  void validFirstNameIsAccepted() {
    assertNull(FirstNameValidator.validate("Антон"));
  }

  @Test
  void checkRejectsEmptyFirstName() {
    Validator validator = new Validator();
    SimpleStringProperty firstName = new SimpleStringProperty("");
    FirstNameValidator.createCheck(validator, firstName);

    assertFalse(validator.validate());
    assertTrue(validator.containsErrors());
    assertEquals(
        ErrorMessages.Required, validator.getValidationResult().getMessages().get(0).getText());
  }

  @Test
  void checkAcceptsValidFirstName() {
    Validator validator = new Validator();
    SimpleStringProperty firstName = new SimpleStringProperty("Антон");
    FirstNameValidator.createCheck(validator, firstName);

    assertTrue(validator.validate());
  }
}
