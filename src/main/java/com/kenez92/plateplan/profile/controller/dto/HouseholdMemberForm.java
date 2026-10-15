package com.kenez92.plateplan.profile.controller.dto;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.kenez92.plateplan.profile.model.ActivityLevel;
import com.kenez92.plateplan.profile.model.Goal;
import com.kenez92.plateplan.profile.model.Sex;

/**
 * The fields of one added person. The display name is required. There is no email. Each product
 * list is unique (first spelling kept, compared case-insensitively). A blank name becomes null so
 * the service can refuse it. The values are personal data, so {@link #toString()} shows none of
 * them.
 */
public record HouseholdMemberForm(String displayName,
                                  Integer age,
                                  Integer heightCm,
                                  BigDecimal weightKg,
                                  Sex sex,
                                  Goal goal,
                                  ActivityLevel activityLevel,
                                  List<String> preferredProducts,
                                  List<String> excludedProducts) {

    private static final String REDACTED = "HouseholdMemberForm[redacted]";

    public HouseholdMemberForm {
        displayName = normalizeDisplayName(displayName);
        preferredProducts = uniqueNames(preferredProducts);
        excludedProducts = uniqueNames(excludedProducts);
    }

    @Override
    public String toString() {
        return REDACTED;
    }

    private static String normalizeDisplayName(final String displayName) {
        if (displayName == null) {
            return null;
        }
        final String normalized = Normalizer.normalize(displayName.strip(), Normalizer.Form.NFC);
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized;
    }

    private static List<String> uniqueNames(final List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        return List.copyOf(names.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        name -> name.toLowerCase(Locale.ROOT),
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new))
                .values());
    }
}
