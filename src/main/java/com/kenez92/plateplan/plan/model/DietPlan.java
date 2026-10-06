package com.kenez92.plateplan.plan.model;

import java.util.List;
import java.util.Objects;

/**
 * A next-day diet: four meals and the shopping list. Structured model output uses this shape.
 */
public record DietPlan(Meal breakfast,
                       Meal secondBreakfast,
                       Meal lunch,
                       Meal dinner,
                       List<ShoppingItem> shoppingList) {

    public DietPlan {
        breakfast = Objects.requireNonNull(breakfast);
        secondBreakfast = Objects.requireNonNull(secondBreakfast);
        lunch = Objects.requireNonNull(lunch);
        dinner = Objects.requireNonNull(dinner);
        shoppingList = shoppingList == null ? List.of() : List.copyOf(shoppingList);
    }
}
