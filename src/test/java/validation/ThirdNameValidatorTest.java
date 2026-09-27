package validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.DesktopApplicationClientJava.validation.ThirdNameValidator;
import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;

public class ThirdNameValidatorTest {
    private static final String FIELD_NAME = ThirdNameValidator.FIELD_NAME;
    private static final ThirdNameValidator thirdNameValidator = new ThirdNameValidator();

    @Test
    void emptyThirdNameIsRejected() {
        assertEquals(ErrorMessages.Required, thirdNameValidator.isValid(""));
    }

    @Test
    void validThirdNameIsAccepted() {
        assertNull(thirdNameValidator.isValid("Антон"));
    }
}
