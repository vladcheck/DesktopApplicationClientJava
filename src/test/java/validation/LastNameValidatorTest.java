package validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.DesktopApplicationClientJava.validation.LastNameValidator;
import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import javafx.beans.property.SimpleStringProperty;
import net.synedra.validatorfx.Validator;
import org.junit.jupiter.api.Test;

public class LastNameValidatorTest extends ValidatorFxTest {

  @Test
  void emptyLastNameIsRejected() {
    assertEquals(ErrorMessages.Required, LastNameValidator.validate(""));
  }

  @Test
  void validLastNameIsAccepted() {
    assertNull(LastNameValidator.validate("Антонов"));
  }

  @Test
  void checkRejectsEmptyLastName() {
    Validator validator = new Validator();
    SimpleStringProperty lastName = new SimpleStringProperty("");
    LastNameValidator.createCheck(validator, lastName);

    assertFalse(validator.validate());
    assertTrue(validator.containsErrors());
    assertEquals(
        ErrorMessages.Required, validator.getValidationResult().getMessages().get(0).getText());
  }

  @Test
  void checkAcceptsValidLastName() {
    Validator validator = new Validator();
    SimpleStringProperty lastName = new SimpleStringProperty("Антонов");
    LastNameValidator.createCheck(validator, lastName);

    assertTrue(validator.validate());
  }
}
