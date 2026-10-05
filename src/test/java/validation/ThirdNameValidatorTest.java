package validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.DesktopApplicationClientJava.validation.ThirdNameValidator;
import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import javafx.beans.property.SimpleStringProperty;
import net.synedra.validatorfx.Validator;
import org.junit.jupiter.api.Test;

public class ThirdNameValidatorTest extends ValidatorFxTest {

  @Test
  void emptyThirdNameIsRejected() {
    assertEquals(ErrorMessages.Required, ThirdNameValidator.validate(""));
  }

  @Test
  void validThirdNameIsAccepted() {
    assertNull(ThirdNameValidator.validate("Антон"));
  }

  @Test
  void checkRejectsEmptyThirdName() {
    Validator validator = new Validator();
    SimpleStringProperty thirdName = new SimpleStringProperty("");
    ThirdNameValidator.createCheck(validator, thirdName);

    assertFalse(validator.validate());
    assertTrue(validator.containsErrors());
    assertEquals(
        ErrorMessages.Required, validator.getValidationResult().getMessages().get(0).getText());
  }

  @Test
  void checkAcceptsValidThirdName() {
    Validator validator = new Validator();
    SimpleStringProperty thirdName = new SimpleStringProperty("Антон");
    ThirdNameValidator.createCheck(validator, thirdName);

    assertTrue(validator.validate());
  }
}
