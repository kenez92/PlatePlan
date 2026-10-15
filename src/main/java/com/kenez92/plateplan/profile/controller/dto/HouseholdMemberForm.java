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
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The fields of one added person. The display name is required. There is no email. Each product
 * list is unique (first spelling kept, compared case-insensitively). A blank name becomes null so
 * the service can refuse it. The values are personal data, so {@link #toString()} shows none of
 * them.
 */
public record HouseholdMemberForm(
        @NotNull(message = "{profile.required}")
        @Size(min = 2, max = 40, message = "{profile.name.invalid}")
        String displayName,
        @NotNull(message = "{profile.required}")
        @Min(value = 2, message = "{profile.age.invalid}")
        @Max(value = 110, message = "{profile.age.invalid}")
        Integer age,
        @NotNull(message = "{profile.required}")
        @Min(value = 70, message = "{profile.height.invalid}")
        @Max(value = 250, message = "{profile.height.invalid}")
        Integer heightCm,
        @NotNull(message = "{profile.required}")
        @DecimalMin(value = "8.0", message = "{profile.weight.invalid}")
        @DecimalMax(value = "400.0", message = "{profile.weight.invalid}")
        @Digits(integer = 3, fraction = 1, message = "{profile.weight.invalid}")
        BigDecimal weightKg,
        @NotNull(message = "{profile.required}")
        Sex sex,
        @NotNull(message = "{profile.required}")
        Goal goal,
        @NotNull(message = "{profile.required}")
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

    /**
     * The form shown before any added person has been typed.
     */
    public static HouseholdMemberForm empty() {
        return new HouseholdMemberForm(null, null, null, null, null, null, null, List.of(), List.of());
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
