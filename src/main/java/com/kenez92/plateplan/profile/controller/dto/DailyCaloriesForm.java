package com.kenez92.plateplan.profile.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * The only field the client may send when editing the daily calorie target. The values are personal
 * data, so {@link #toString()} shows none of them.
 */
public record DailyCaloriesForm(
        @NotNull(message = "{profile.required}")
        @Min(value = 800, message = "{profile.calories.invalid}")
        @Max(value = 6000, message = "{profile.calories.invalid}")
        Integer dailyCalories) {

    private static final String REDACTED = "DailyCaloriesForm[redacted]";

    public static DailyCaloriesForm empty() {
        return new DailyCaloriesForm(null);
    }

    @Override
    public String toString() {
        return REDACTED;
    }
}
