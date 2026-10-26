package com.kenez92.plateplan.config;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Locale;

import org.springframework.format.Formatter;

/**
 * Reads a decimal whether the browser sent a point or a comma, and prints a point. A Polish page
 * shows a stored weight with a point, and a later save must accept that point and a typed comma.
 * The message of a refusal does not include the typed text.
 */
public class DecimalFormatter implements Formatter<BigDecimal> {

    private static final String INVALID = "Invalid decimal";
    private static final String EMPTY = "";
    private static final char COMMA = ',';
    private static final char POINT = '.';
    private static final int NO_OFFSET = 0;
    private static final int DIGIT_ZERO = '0';
    private static final int DIGIT_NINE = '9';

    @Override
    public BigDecimal parse(final String text, final Locale locale) throws ParseException {
        final String normalized = text == null ? EMPTY : text.strip().replace(COMMA, POINT);
        if (normalized.isEmpty() || normalized.codePoints().anyMatch(this::refused)) {
            throw new ParseException(INVALID, NO_OFFSET);
        }
        try {
            return new BigDecimal(normalized);
        } catch (final NumberFormatException exception) {
            throw new ParseException(INVALID, NO_OFFSET);
        }
    }

    @Override
    public String print(final BigDecimal value, final Locale locale) {
        return value == null ? EMPTY : value.toPlainString();
    }

    private boolean refused(final int codePoint) {
        return codePoint != POINT && (codePoint < DIGIT_ZERO || codePoint > DIGIT_NINE);
    }
}
