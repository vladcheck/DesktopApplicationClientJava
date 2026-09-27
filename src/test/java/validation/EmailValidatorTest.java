package validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.DesktopApplicationClientJava.validation.EmailValidator;
import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;

public class EmailValidatorTest {
    private static final String FIELD_NAME = EmailValidator.FIELD_NAME;
    private static final EmailValidator emailValidator = new EmailValidator();

    @Test
    void emptyEmailIsRejected() {
        assertEquals(ErrorMessages.Required, emailValidator.isValid(""));
    }

    @Test
    void blankEmailIsRejected() {
        assertEquals(ErrorMessages.Required, emailValidator.isValid("   "));
    }

    @Test
    void shortEmailIsRejected() {
        assertEquals(ErrorMessages.MinLength(FIELD_NAME, EmailValidator.MIN_EMAIL_LENGTH),
                emailValidator.isValid("a@b"));
    }

    @Test
    void longEmailIsRejected() {
        String e = "a".repeat(95) + "@x.com";
        assertEquals(ErrorMessages.MaxLength(FIELD_NAME, EmailValidator.MAX_EMAIL_LENGTH), emailValidator.isValid(e));
    }

    @Test
    void emailWithoutAtIsRejected() {
        assertEquals(ErrorMessages.IncorrectFormat, emailValidator.isValid("nope.com"));
    }

    @Test
    void validEmailIsAccepted() {
        assertNull(emailValidator.isValid("test@example.com"));
    }
}
