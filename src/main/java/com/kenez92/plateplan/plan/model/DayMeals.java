package com.kenez92.plateplan.plan.model;

import java.util.Objects;

/**
 * Four meals of one day. None of them is null. The shopping list belongs to the week.
 */
public record DayMeals(Meal breakfast,
                       Meal secondBreakfast,
                       Meal lunch,
                       Meal dinner) {

    public DayMeals {
        breakfast = Objects.requireNonNull(breakfast);
        secondBreakfast = Objects.requireNonNull(secondBreakfast);
        lunch = Objects.requireNonNull(lunch);
        dinner = Objects.requireNonNull(dinner);
    }
}
