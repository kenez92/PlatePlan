package com.kenez92.plateplan.profile.validator;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * The display name of the owner or of an added person, as the client sent it. A name has 2 to 40
 * characters, is already stripped and composed (NFC), has no run of ordinary spaces, and may not
 * contain control or format characters or any space other than the ordinary one. A semicolon is
 * allowed. The name is not rewritten; a value that is not already in that form is refused. Null is
 * not a name: the owner may omit one, and that omission matches as {@code Ja}.
 */
@Component
public class DisplayNameValidator {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 40;
    private static final int FIRST_CHARACTER_INDEX = 0;
    private static final int ORDINARY_SPACE = ' ';
    private static final String UNNAMED_OWNER = "Ja";
    private static final Pattern SPACE_RUN = Pattern.compile(" {2,}");

    public boolean isAcceptable(final String name) {
        return name != null
                && name.equals(name.strip())
                && name.equals(Normalizer.normalize(name, Normalizer.Form.NFC))
                && !SPACE_RUN.matcher(name).find()
                && isWithin(name.codePointCount(FIRST_CHARACTER_INDEX, name.length()))
                && name.codePoints().noneMatch(this::isForbidden);
    }

    /**
     * The key used to decide that two names are the same. A missing owner name matches as {@code Ja}.
     * Comparison strips and ignores case.
     */
    public String matchingKey(final String displayName) {
        return (displayName == null || displayName.isBlank() ? UNNAMED_OWNER : displayName)
                .strip()
                .toLowerCase(Locale.ROOT);
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
