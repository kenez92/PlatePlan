package com.kenez92.plateplan.plan.validator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import com.kenez92.plateplan.plan.model.DayMeals;
import com.kenez92.plateplan.plan.model.Meal;
import com.kenez92.plateplan.plan.model.PlanPerson;
import com.kenez92.plateplan.plan.model.Portion;
import com.kenez92.plateplan.plan.model.WeekPlan;
import org.springframework.stereotype.Component;

/**
 * Whether a week may be turned into a PDF. An accepted week has exactly seven days. Every meal has
 * a non-empty name and exactly one portion for each selected person, with no extra portion. The sum
 * of a person's four portions sits inside ten percent of that person's target. Breakfast, lunch,
 * and dinner names are unique. A repeated second breakfast is accepted. The shopping list is not
 * checked. Nothing from the plan is logged.
 */
@Component
public class WeekPlanValidator {

    private static final int DAYS_IN_WEEK = 7;
    private static final BigDecimal TEN_PERCENT = new BigDecimal("0.10");
    /**
     * Decimal places of a whole kilocalorie. The band has no fractional part.
     */
    private static final int WHOLE_KILOCALORIE_SCALE = 0;

    public boolean isValid(final WeekPlan weekPlan, final List<PlanPerson> people) {
        if (weekPlan == null || people == null || people.isEmpty() || !hasSevenDays(weekPlan)) {
            return false;
        }
        return weekPlan.days().stream().allMatch((final DayMeals day) -> dayAccepted(day, people))
                && cookedNamesAreUnique(weekPlan);
    }

    private boolean hasSevenDays(final WeekPlan weekPlan) {
        final List<DayMeals> days = weekPlan.days();
        return days != null && days.size() == DAYS_IN_WEEK;
    }

    private boolean dayAccepted(final DayMeals day, final List<PlanPerson> people) {
        return named(day) && portionsMatch(day, people) && bandsHold(day, people);
    }

    private boolean named(final DayMeals day) {
        return Stream.of(day.breakfast().name(), day.secondBreakfast().name(), day.lunch().name(), day.dinner().name())
                .allMatch(this::hasName);
    }

    private boolean hasName(final String name) {
        return !nameKey(name).isEmpty();
    }

    private boolean portionsMatch(final DayMeals day, final List<PlanPerson> people) {
        return Stream.of(day.breakfast(), day.secondBreakfast(), day.lunch(), day.dinner())
                .allMatch((final Meal meal) -> portionsMatch(meal, people));
    }

    private boolean portionsMatch(final Meal meal, final List<PlanPerson> people) {
        final List<String> keys = meal.portions().stream()
                .map((final Portion portion) -> nameKey(portion.personName()))
                .toList();
        return keys.size() == people.size()
                && keys.stream().distinct().count() == keys.size()
                && people.stream().allMatch((final PlanPerson person) -> keys.contains(nameKey(person.name())));
    }

    private boolean bandsHold(final DayMeals day, final List<PlanPerson> people) {
        return people.stream().allMatch((final PlanPerson person) -> caloriesInBand(day, person));
    }

    private boolean caloriesInBand(final DayMeals day, final PlanPerson person) {
        final int total = Stream.of(day.breakfast(), day.secondBreakfast(), day.lunch(), day.dinner())
                .mapToInt((final Meal meal) -> portionKcal(meal, person))
                .sum();
        final int delta = bandDelta(person.kcal());
        return total >= person.kcal() - delta && total <= person.kcal() + delta;
    }

    private int portionKcal(final Meal meal, final PlanPerson person) {
        return meal.portions().stream()
                .filter((final Portion portion) -> nameKey(portion.personName()).equals(nameKey(person.name())))
                .mapToInt(Portion::kcal)
                .sum();
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
