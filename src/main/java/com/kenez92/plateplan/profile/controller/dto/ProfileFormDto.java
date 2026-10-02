package com.kenez92.plateplan.profile.controller.dto;

import java.math.BigDecimal;

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
import org.apache.commons.lang3.StringUtils;

/**
 * The profile fields of the form. The client sends values already in the stored form; Bean
 * Validation refuses a missing or out-of-range body field, a value Spring cannot bind is a field
 * error, and {@link ProductListsValidator} refuses a bad product list. Nothing here rewrites what
 * arrived. The values are personal data, so {@link #toString()} shows none of them.
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
        String preferredProducts,
        String excludedProducts) {

    private static final String REDACTED = "ProfileFormDto[redacted]";

    /**
     * The form of an account that has not saved a profile yet.
     */
    public static ProfileFormDto empty() {
        return new ProfileFormDto(null, null, null, null, null, null, StringUtils.EMPTY, StringUtils.EMPTY);
    }

    @Override
    public String toString() {
        return REDACTED;
    }
}
