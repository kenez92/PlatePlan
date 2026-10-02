package com.kenez92.plateplan.account;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * The rules a login and a password must meet before an account is created. The login is judged as
 * it will be stored, so the caller normalizes it first. Beyond its length it may not contain
 * control or format characters or any space other than the ordinary one, which would allow logins
 * that look alike. The password is not trimmed. It is capped in bytes, because BCrypt silently
 * drops everything after the 72nd byte.
 */
@Component
public class RegistrationValidator {

    private static final int LOGIN_MIN_LENGTH = 3;
    private static final int LOGIN_MAX_LENGTH = 50;
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_BYTES = 72;
    private static final int FIRST_CHARACTER_INDEX = 0;
    private static final int ORDINARY_SPACE = ' ';

    public Optional<RegistrationError> validate(final String login, final String password) {
        if (!isLoginValid(login)) {
            return Optional.of(RegistrationError.LOGIN_INVALID);
        }
        if (!isPasswordValid(password)) {
            return Optional.of(RegistrationError.PASSWORD_INVALID);
        }
        return Optional.empty();
    }

    private boolean isLoginValid(final String login) {
        return login != null
                && isWithin(lengthInCharacters(login), LOGIN_MIN_LENGTH, LOGIN_MAX_LENGTH)
                && login.codePoints().noneMatch(this::isForbiddenInLogin);
    }

    private boolean isForbiddenInLogin(final int codePoint) {
        return Character.isISOControl(codePoint)
                || Character.getType(codePoint) == Character.FORMAT
                || (Character.isSpaceChar(codePoint) && codePoint != ORDINARY_SPACE);
    }

    private boolean isPasswordValid(final String password) {
        return password != null
                && lengthInCharacters(password) >= PASSWORD_MIN_LENGTH
                && password.getBytes(StandardCharsets.UTF_8).length <= PASSWORD_MAX_BYTES;
    }

    private int lengthInCharacters(final String text) {
        return text.codePointCount(FIRST_CHARACTER_INDEX, text.length());
    }

    private boolean isWithin(final int value, final int min, final int max) {
        return value >= min && value <= max;
    }
}
