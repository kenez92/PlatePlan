package com.kenez92.plateplan.profile.validator;

import java.text.Normalizer;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * The rule for one product name as the client sent it. A name has 2 to 50 characters, is already
 * stripped and composed (NFC), has no run of ordinary spaces, and may not contain the list
 * separator, control or format characters, or any space other than the ordinary one. A comma is
 * allowed. The name is not rewritten; a value that is not already in that form is refused.
 */
@Component
public class ProductNameValidator {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 50;
    private static final int FIRST_CHARACTER_INDEX = 0;
    private static final int ORDINARY_SPACE = ' ';
    private static final int SEPARATOR = ';';
    private static final Pattern SPACE_RUN = Pattern.compile(" {2,}");

    public boolean isValid(final String name) {
        return name != null
                && name.equals(name.strip())
                && name.equals(Normalizer.normalize(name, Normalizer.Form.NFC))
                && !SPACE_RUN.matcher(name).find()
                && isWithin(name.codePointCount(FIRST_CHARACTER_INDEX, name.length()))
                && name.codePoints().noneMatch(this::isForbidden);
    }

    private boolean isForbidden(final int codePoint) {
        return codePoint == SEPARATOR
                || Character.isISOControl(codePoint)
                || Character.getType(codePoint) == Character.FORMAT
                || (Character.isSpaceChar(codePoint) && codePoint != ORDINARY_SPACE);
    }

    private boolean isWithin(final int length) {
        return length >= MIN_LENGTH && length <= MAX_LENGTH;
    }
}
