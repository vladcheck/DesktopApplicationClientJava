package validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.DesktopApplicationClientJava.validation.LastNameValidator;
import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.StringField;
import com.dlsc.formsfx.model.validators.Validator;
import java.util.List;
import org.junit.jupiter.api.Test;

public class LastNameValidatorTest {

  private static StringField fieldWith(String value) {
    List<Validator<String>> validators = LastNameValidator.validators();
    @SuppressWarnings("unchecked")
    Validator<String>[] array = validators.toArray(new Validator[0]);
    return Field.ofStringType(value).required(ErrorMessages.Required).validate(array);
  }

  @Test
  void emptyLastNameIsRejected() {
    assertEquals(ErrorMessages.Required, LastNameValidator.validate(""));
  }

  @Test
  void validLastNameIsAccepted() {
    assertNull(LastNameValidator.validate("Антонов"));
  }

  @Test
  void fieldRejectsEmptyLastName() {
    StringField field = fieldWith("");

    assertFalse(field.isValid());
    assertTrue(field.getErrorMessages().contains(ErrorMessages.Required));
  }

  @Test
  void fieldAcceptsValidLastName() {
    assertTrue(fieldWith("Антонов").isValid());
  }
}
