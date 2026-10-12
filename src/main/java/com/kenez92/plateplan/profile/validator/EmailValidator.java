package com.kenez92.plateplan.profile.validator;

import java.text.Normalizer;
import java.util.regex.Pattern;

import com.kenez92.plateplan.profile.controller.dto.ProfileFormDto;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * The optional delivery address on {@link ProfileFormDto}. Blank is allowed and stored as null.
 * A non-blank value must already be stripped and NFC, fit in 3 to 254 characters, look like an
 * address with one {@code @}, and contain no control, format, or non-ordinary space.
 */
@Component
public class EmailValidator implements Validator {

    private static final String EMAIL_FIELD = "email";
    private static final String INVALID = "profile.email.invalid";
    private static final Object[] NO_MESSAGE_ARGUMENTS = new Object[0];
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 254;
    private static final int FIRST_CHARACTER_INDEX = 0;
    private static final int ORDINARY_SPACE = ' ';
    private static final Pattern ADDRESS = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @Override
    public boolean supports(final Class<?> type) {
        return ProfileFormDto.class.isAssignableFrom(type);
    }

    @Override
    public void validate(final Object target, final Errors errors) {
        final String email = ((ProfileFormDto) target).email();
        if (!isValid(email)) {
            errors.rejectValue(EMAIL_FIELD, INVALID, NO_MESSAGE_ARGUMENTS, INVALID);
        }
    }

    public boolean isValid(final String email) {
        return email == null
                || (email.equals(email.strip())
                && email.equals(Normalizer.normalize(email, Normalizer.Form.NFC))
                && isWithin(email.codePointCount(FIRST_CHARACTER_INDEX, email.length()))
                && ADDRESS.matcher(email).matches()
                && email.codePoints().noneMatch(this::isForbidden));
    }

    private boolean isForbidden(final int codePoint) {
        return Character.isISOControl(codePoint)
                || Character.getType(codePoint) == Character.FORMAT
                || (Character.isSpaceChar(codePoint) && codePoint != ORDINARY_SPACE);
    }

    private boolean isWithin(final int length) {
        return length >= MIN_LENGTH && length <= MAX_LENGTH;
    }
}
