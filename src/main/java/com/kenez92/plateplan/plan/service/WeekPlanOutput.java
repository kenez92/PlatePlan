package com.kenez92.plateplan.plan.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import com.kenez92.plateplan.plan.model.DayMeals;
import com.kenez92.plateplan.plan.model.ShoppingItem;
import com.kenez92.plateplan.plan.model.WeekPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.stereotype.Component;

/**
 * Reads the model reply as a {@link WeekPlan}. A complete object is parsed as a whole. A reply that
 * stops mid-day still keeps every day object that was finished, and a decimal or a quoted macro is
 * rounded to a whole number first. A comma inside a dish text stays. Nothing from the reply is logged.
 */
@Component
public class WeekPlanOutput implements StructuredOutputConverter<WeekPlan> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeekPlanOutput.class);

    private static final String UNREADABLE_LOG = "Diet reply unreadable: {} characters";
    private static final String DAYS = "days";
    private static final String SHOPPING_LIST = "shoppingList";
    private static final String FENCE = "```";
    private static final char OBJECT_OPEN = '{';
    private static final char OBJECT_CLOSE = '}';
    private static final char ARRAY_CLOSE = ']';
    private static final char QUOTE = '"';
    private static final char ESCAPE = '\\';
    private static final int NOT_FOUND = -1;
    private static final int WHOLE_NUMBER = 0;
    private static final int FIRST = 0;
    private static final String MACRO_KEY = "\"%s\": ";
    private static final Pattern DECIMAL_MACRO = Pattern.compile(
            "\"(kcal|proteinG|carbsG|fatG)\"\\s*:\\s*(-?\\d+\\.\\d+)");
    private static final Pattern QUOTED_MACRO = Pattern.compile(
            "\"(kcal|proteinG|carbsG|fatG)\"\\s*:\\s*\"(-?\\d+(?:\\.\\d+)?)\"");
    private static final Pattern BARE_AMOUNT = Pattern.compile("\"amount\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
    private static final String AMOUNT_VALUE = "\"amount\": \"%s\"";

    private final BeanOutputConverter<WeekPlan> week = new BeanOutputConverter<>(WeekPlan.class);
    private final BeanOutputConverter<DayMeals> day = new BeanOutputConverter<>(DayMeals.class);
    private final BeanOutputConverter<ShoppingItem> item = new BeanOutputConverter<>(ShoppingItem.class);

    @Override
    public String getFormat() {
        return week.getFormat();
    }

    @Override
    public String getJsonSchema() {
        return week.getJsonSchema();
    }

    @Override
    public WeekPlan convert(final String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        final String prepared = soften(unwrap(text));
        try {
            return week.convert(prepared);
        } catch (final RuntimeException exception) {
            final WeekPlan partial = partial(prepared);
            if (partial == null) {
                LOGGER.warn(UNREADABLE_LOG, prepared.length());
                throw exception;
            }
            return partial;
        }
    }

    private String unwrap(final String text) {
        final String trimmed = text.strip();
        if (!trimmed.startsWith(FENCE)) {
            return trimmed;
        }
        final int lineBreak = trimmed.indexOf('\n');
        final String body = lineBreak < FIRST ? trimmed : trimmed.substring(lineBreak + 1);
        if (!body.stripTrailing().endsWith(FENCE)) {
            return body.strip();
        }
        final String withoutFence = body.stripTrailing();
        return withoutFence.substring(FIRST, withoutFence.length() - FENCE.length()).strip();
    }

    private String soften(final String json) {
        return BARE_AMOUNT.matcher(QUOTED_MACRO.matcher(DECIMAL_MACRO.matcher(withoutTrailingCommas(json))
                        .replaceAll(this::roundedMacro))
                .replaceAll(this::roundedMacro))
                .replaceAll(this::quotedAmount);
    }

    private String withoutTrailingCommas(final String json) {
        final StringBuilder out = new StringBuilder(json.length());
        boolean inString = false;
        boolean escaped = false;
        for (int index = 0; index < json.length(); index++) {
            final char current = json.charAt(index);
            if (inString) {
                out.append(current);
                if (escaped) {
                    escaped = false;
                } else if (current == ESCAPE) {
                    escaped = true;
                } else if (current == QUOTE) {
                    inString = false;
                }
                continue;
            }
            if (current == QUOTE) {
                inString = true;
                out.append(current);
                continue;
            }
            if (current == ',' && closesAfterWhitespace(json, index + 1)) {
                continue;
            }
            out.append(current);
        }
        return out.toString();
    }

    private boolean closesAfterWhitespace(final String json, final int start) {
        final int next = IntStream.range(start, json.length())
                .filter((final int index) -> !Character.isWhitespace(json.charAt(index)))
                .findFirst()
                .orElse(json.length());
        if (next >= json.length()) {
            return false;
        }
        final char closer = json.charAt(next);
        return closer == OBJECT_CLOSE || closer == ARRAY_CLOSE;
    }

    private String roundedMacro(final MatchResult matcher) {
        return MACRO_KEY.formatted(matcher.group(1))
                + new BigDecimal(matcher.group(2)).setScale(WHOLE_NUMBER, RoundingMode.HALF_UP).toPlainString();
    }

    private String quotedAmount(final MatchResult matcher) {
        return AMOUNT_VALUE.formatted(matcher.group(1));
    }

    private WeekPlan partial(final String json) {
        final List<DayMeals> days = objects(json, DAYS).stream()
                .map(this::readDay)
                .flatMap(Optional::stream)
                .toList();
        if (days.isEmpty()) {
            return null;
        }
        return new WeekPlan(days, objects(json, SHOPPING_LIST).stream()
                .map(this::readItem)
                .flatMap(Optional::stream)
                .toList());
    }

    private Optional<DayMeals> readDay(final String json) {
        try {
            final DayMeals parsed = day.convert(json);
            return parsed == null ? Optional.empty() : Optional.of(parsed);
        } catch (final RuntimeException exception) {
            return Optional.empty();
        }
    }

    private Optional<ShoppingItem> readItem(final String json) {
        try {
            final ShoppingItem parsed = item.convert(json);
            return parsed == null ? Optional.empty() : Optional.of(parsed);
        } catch (final RuntimeException exception) {
            return Optional.empty();
        }
    }

    private List<String> objects(final String json, final String field) {
        final int fieldAt = json.indexOf(QUOTE + field + QUOTE);
        if (fieldAt < FIRST) {
            return List.of();
        }
        final int arrayAt = json.indexOf('[', fieldAt);
        if (arrayAt < FIRST) {
            return List.of();
        }
        return completeObjects(json, arrayAt + 1);
    }

    private List<String> completeObjects(final String json, final int start) {
        final List<String> found = new ArrayList<>();
        int index = start;
        while (index < json.length()) {
            final char current = json.charAt(index);
            if (current == ARRAY_CLOSE) {
                return List.copyOf(found);
            }
            if (current != OBJECT_OPEN) {
                index++;
                continue;
            }
            final int end = endOfObject(json, index);
            if (end == NOT_FOUND) {
                return List.copyOf(found);
            }
            found.add(json.substring(index, end + 1));
            index = end + 1;
        }
        return List.copyOf(found);
    }

    private int endOfObject(final String json, final int open) {
        int depth = FIRST;
        boolean inString = false;
        boolean escaped = false;
        for (int index = open; index < json.length(); index++) {
            final char current = json.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == ESCAPE) {
                    escaped = true;
                } else if (current == QUOTE) {
                    inString = false;
                }
                continue;
            }
            if (current == QUOTE) {
                inString = true;
            } else if (current == OBJECT_OPEN) {
                depth++;
            } else if (current == OBJECT_CLOSE) {
                depth--;
                if (depth == FIRST) {
                    return index;
                }
            }
        }
        return NOT_FOUND;
    }
}
