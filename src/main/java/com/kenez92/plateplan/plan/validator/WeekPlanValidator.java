package com.kenez92.plateplan.plan.validator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import com.kenez92.plateplan.plan.model.DayMeals;
import com.kenez92.plateplan.plan.model.WeekPlan;
import org.springframework.stereotype.Component;

/**
 * Whether a week may be turned into a PDF. An accepted week has exactly seven days, a non-empty
 * name on every meal, each day's kilocalories inside ten percent of the daily target, and unique
 * breakfast, lunch, and dinner names. A repeated second breakfast is accepted. The shopping list
 * is not checked.
 */
@Component
public class WeekPlanValidator {

    private static final int DAYS_IN_WEEK = 7;
    private static final BigDecimal TEN_PERCENT = new BigDecimal("0.10");
    /**
     * Decimal places of a whole kilocalorie. The band has no fractional part.
     */
    private static final int WHOLE_KILOCALORIE_SCALE = 0;

    public boolean isValid(final WeekPlan weekPlan, final int dailyCalories) {
        if (weekPlan == null || !hasSevenDays(weekPlan)) {
            return false;
        }
        return weekPlan.days().stream().allMatch((final DayMeals day) -> dayAccepted(day, dailyCalories))
                && cookedNamesAreUnique(weekPlan);
    }

    private boolean hasSevenDays(final WeekPlan weekPlan) {
        final List<DayMeals> days = weekPlan.days();
        return days != null && days.size() == DAYS_IN_WEEK;
    }

    private boolean dayAccepted(final DayMeals day, final int dailyCalories) {
        return named(day) && caloriesInBand(day, dailyCalories);
    }

    private boolean named(final DayMeals day) {
        return Stream.of(day.breakfast().name(), day.secondBreakfast().name(), day.lunch().name(), day.dinner().name())
                .allMatch(this::hasName);
    }

    private boolean hasName(final String name) {
        return !nameKey(name).isEmpty();
    }

    private boolean caloriesInBand(final DayMeals day, final int dailyCalories) {
        final int total = day.breakfast().kcal()
                + day.secondBreakfast().kcal()
                + day.lunch().kcal()
                + day.dinner().kcal();
        final int delta = bandDelta(dailyCalories);
        return total >= dailyCalories - delta && total <= dailyCalories + delta;
    }

    private int bandDelta(final int dailyCalories) {
        return new BigDecimal(dailyCalories)
                .multiply(TEN_PERCENT)
                .setScale(WHOLE_KILOCALORIE_SCALE, RoundingMode.HALF_UP)
                .intValueExact();
    }

    private boolean cookedNamesAreUnique(final WeekPlan weekPlan) {
        final List<String> keys = weekPlan.days().stream()
                .flatMap((final DayMeals day) -> Stream.of(
                        nameKey(day.breakfast().name()),
                        nameKey(day.lunch().name()),
                        nameKey(day.dinner().name())))
                .toList();
        return keys.stream().distinct().count() == keys.size();
    }

    private String nameKey(final String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }
}
