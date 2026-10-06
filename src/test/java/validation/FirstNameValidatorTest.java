package validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.DesktopApplicationClientJava.validation.FirstNameValidator;
import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.StringField;
import com.dlsc.formsfx.model.validators.Validator;
import java.util.List;
import org.junit.jupiter.api.Test;

public class FirstNameValidatorTest {

  private static StringField fieldWith(String value) {
    List<Validator<String>> validators = FirstNameValidator.validators();
    @SuppressWarnings("unchecked")
    Validator<String>[] array = validators.toArray(new Validator[0]);
    return Field.ofStringType(value).required(ErrorMessages.Required).validate(array);
  }

  @Test
  void emptyFirstNameIsRejected() {
    assertEquals(ErrorMessages.Required, FirstNameValidator.validate(""));
  }

  @Test
  void validFirstNameIsAccepted() {
    assertNull(FirstNameValidator.validate("Антон"));
  }

  @Test
  void fieldRejectsEmptyFirstName() {
    StringField field = fieldWith("");

    assertFalse(field.isValid());
    assertTrue(field.getErrorMessages().contains(ErrorMessages.Required));
  }

  @Test
  void fieldAcceptsValidFirstName() {
    assertTrue(fieldWith("Антон").isValid());
  }
}
