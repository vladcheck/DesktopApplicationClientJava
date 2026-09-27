package validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.DesktopApplicationClientJava.validation.LastNameValidator;
import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;

public class LastNameValidatorTest {
    private static final String FIELD_NAME = LastNameValidator.FIELD_NAME;
    private static final LastNameValidator lastNameValidator = new LastNameValidator();

    @Test
    void emptyLastNameIsRejected() {
        assertEquals(ErrorMessages.Required, lastNameValidator.isValid(""));
    }

    @Test
    void validLastNameIsAccepted() {
        assertNull(lastNameValidator.isValid("Антонов"));
    }
}
