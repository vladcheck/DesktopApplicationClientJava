package validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.DesktopApplicationClientJava.validation.FirstNameValidator;
import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;

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
