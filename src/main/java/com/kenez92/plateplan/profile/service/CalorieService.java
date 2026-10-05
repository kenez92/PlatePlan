package com.kenez92.plateplan.profile.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.kenez92.plateplan.profile.model.ActivityLevel;
import com.kenez92.plateplan.profile.model.Goal;
import com.kenez92.plateplan.profile.model.Sex;
import org.springframework.stereotype.Service;

/**
 * The daily calorie number from BMR, activity, and goal. The six formula fields are the only
 * inputs; product preferences are not a parameter. The profile shows this number as the proposed
 * daily calories.
 */
@Service
public class CalorieService {

    private static final BigDecimal WEIGHT_FACTOR = new BigDecimal("10");
    private static final BigDecimal HEIGHT_FACTOR = new BigDecimal("6.25");
    private static final BigDecimal AGE_FACTOR = new BigDecimal("5");
    private static final BigDecimal MALE_OFFSET = new BigDecimal("5");
    private static final BigDecimal FEMALE_OFFSET = new BigDecimal("-161");
    private static final BigDecimal SEDENTARY_MULTIPLIER = new BigDecimal("1.2");
    private static final BigDecimal LIGHT_MULTIPLIER = new BigDecimal("1.375");
    private static final BigDecimal MODERATE_MULTIPLIER = new BigDecimal("1.55");
    private static final BigDecimal HIGH_MULTIPLIER = new BigDecimal("1.725");
    private static final BigDecimal LOSE_WEIGHT_ADJUSTMENT = new BigDecimal("-500");
    private static final BigDecimal MAINTAIN_ADJUSTMENT = new BigDecimal("0");
    private static final BigDecimal GAIN_ADJUSTMENT = new BigDecimal("500");
    /**
     * Decimal places of a whole kilocalorie. The daily number has no fractional part.
     */
    private static final int WHOLE_KILOCALORIE_SCALE = 0;

    public int dailyCalories(final int age,
                             final int heightCm,
                             final BigDecimal weightKg,
                             final Sex sex,
                             final ActivityLevel activityLevel,
                             final Goal goal) {
        final BigDecimal bmr = weightKg.multiply(WEIGHT_FACTOR)
                .add(HEIGHT_FACTOR.multiply(new BigDecimal(heightCm)))
                .subtract(AGE_FACTOR.multiply(new BigDecimal(age)))
                .add(sexOffset(sex));
        final BigDecimal withActivity = bmr.multiply(activityMultiplier(activityLevel));
        final BigDecimal withGoal = withActivity.add(goalAdjustment(goal));
        return withGoal.setScale(WHOLE_KILOCALORIE_SCALE, RoundingMode.HALF_UP).intValueExact();
    }

    private BigDecimal sexOffset(final Sex sex) {
        return switch (sex) {
            case MALE -> MALE_OFFSET;
            case FEMALE -> FEMALE_OFFSET;
        };
    }

    private BigDecimal activityMultiplier(final ActivityLevel activityLevel) {
        return switch (activityLevel) {
            case SEDENTARY -> SEDENTARY_MULTIPLIER;
            case LIGHT -> LIGHT_MULTIPLIER;
            case MODERATE -> MODERATE_MULTIPLIER;
            case HIGH -> HIGH_MULTIPLIER;
        };
    }

    private BigDecimal goalAdjustment(final Goal goal) {
        return switch (goal) {
            case LOSE_WEIGHT -> LOSE_WEIGHT_ADJUSTMENT;
            case MAINTAIN -> MAINTAIN_ADJUSTMENT;
            case GAIN -> GAIN_ADJUSTMENT;
        };
    }
}
