package validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.DesktopApplicationClientJava.validation.EmailValidator;
import com.DesktopApplicationClientJava.validation.PasswordValidator;

class LoginValidatorTest {
    private static final PasswordValidator passwordValidator = new PasswordValidator();
    private static final EmailValidator emailValidator = new EmailValidator();

    @Test
    void emptyEmailIsRejected() {
        assertEquals("Это поле обязательно для заполнения", emailValidator.isValid(""));
    }

    @Test
    void blankEmailIsRejected() {
        assertEquals("Это поле обязательно для заполнения", emailValidator.isValid("   "));
    }

    @Test
    void shortEmailIsRejected() {
        assertEquals("Почта не может быть короче 5 символов", emailValidator.isValid("a@b"));
    }

    @Test
    void longEmailIsRejected() {
        String e = "a".repeat(95) + "@x.com";
        assertEquals("Почта не должна быть длиннее 100 символов", emailValidator.isValid(e));
    }

    @Test
    void emailWithoutAtIsRejected() {
        assertEquals("Неверный формат почты", emailValidator.isValid("nope.com"));
    }

    @Test
    void validEmailIsAccepted() {
        assertNull(emailValidator.isValid("test@example.com"));
    }

    @Test
    void emptyPasswordIsRejected() {
        assertEquals("Это поле обязательно для заполнения", passwordValidator.isValid(""));
    }

    @Test
    void shortPasswordIsRejected() {
        assertEquals("Пароль не может быть короче 8 символов", passwordValidator.isValid("short"));
    }

    @Test
    void exactlyEightCharPasswordIsAccepted() {
        assertNull(passwordValidator.isValid("12345678"));
    }
}