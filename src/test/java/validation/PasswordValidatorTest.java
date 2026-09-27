package validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.DesktopApplicationClientJava.validation.PasswordValidator;
import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;

public class PasswordValidatorTest {
    private static final String FIELD_NAME = PasswordValidator.FIELD_NAME;
    private static final PasswordValidator passwordValidator = new PasswordValidator();

    @Test
    void emptyPasswordIsRejected() {
        assertEquals(ErrorMessages.Required, passwordValidator.isValid(""));
    }

    @Test
    void shortPasswordIsRejected() {
        assertEquals(ErrorMessages.MinLength(FIELD_NAME, PasswordValidator.MIN_LENGTH),
                passwordValidator.isValid("short"));
    }

    @Test
    void exactlyEightCharPasswordIsAccepted() {
        assertNull(passwordValidator.isValid("12345678"));
    }
}
