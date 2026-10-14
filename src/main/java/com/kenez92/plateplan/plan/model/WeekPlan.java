package com.kenez92.plateplan.plan.model;

import java.util.List;

/**
 * Days and one shopping list for the whole week. A missing day list stays missing so a wrong
 * length is still visible. Structured model output uses this shape.
 */
public record WeekPlan(List<DayMeals> days,
                       List<ShoppingItem> shoppingList) {

    public WeekPlan {
        days = days == null ? null : List.copyOf(days);
        shoppingList = shoppingList == null ? List.of() : List.copyOf(shoppingList);
    }
}
