package com.kenez92.plateplan.profile.controller.dto;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.kenez92.plateplan.profile.model.ActivityLevel;
import com.kenez92.plateplan.profile.model.Goal;
import com.kenez92.plateplan.profile.model.Sex;
import com.kenez92.plateplan.profile.validator.ProductListsValidator;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * The profile fields of the form. The client sends values already in the stored form; Bean
 * Validation refuses a missing or out-of-range body field, a value Spring cannot bind is a field
 * error, and {@link ProductListsValidator} refuses a bad product list. Each product list is unique
 * (first spelling kept, compared case-insensitively). Daily calories are {@link DailyCaloriesForm},
 * not this form. The values are personal data, so {@link #toString()} shows none of them.
 */
public record ProfileFormDto(
        @NotNull(message = "{profile.required}")
        @Min(value = 10, message = "{profile.age.invalid}")
        @Max(value = 110, message = "{profile.age.invalid}")
        Integer age,
        @NotNull(message = "{profile.required}")
        @Min(value = 80, message = "{profile.height.invalid}")
        @Max(value = 250, message = "{profile.height.invalid}")
        Integer heightCm,
        @NotNull(message = "{profile.required}")
        @DecimalMin(value = "20.0", message = "{profile.weight.invalid}")
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

    private static final String REDACTED = "ProfileFormDto[redacted]";

    public ProfileFormDto {
        preferredProducts = uniqueNames(preferredProducts);
        excludedProducts = uniqueNames(excludedProducts);
    }

    /**
     * The form of an account that has not saved a profile yet.
     */
    public static ProfileFormDto empty() {
        return new ProfileFormDto(null, null, null, null, null, null, List.of(), List.of());
    }

    @Override
    public String toString() {
        return REDACTED;
    }

    private static List<String> uniqueNames(final List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        final Map<String, String> unique = new LinkedHashMap<>();
        for (final String name : names) {
            if (name != null) {
                unique.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
            }
        }
        return List.copyOf(unique.values());
    }
}
