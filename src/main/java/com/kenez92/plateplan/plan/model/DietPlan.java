package com.kenez92.plateplan.plan.model;

import java.util.List;

/**
 * A next-day diet: four meals and the shopping list. Structured model output uses this shape.
 */
public record DietPlan(Meal breakfast,
                       Meal secondBreakfast,
                       Meal lunch,
                       Meal dinner,
                       List<String> shoppingList) {

    public DietPlan {
        shoppingList = shoppingList == null ? List.of() : List.copyOf(shoppingList);
    }
}
