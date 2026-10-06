package com.kenez92.plateplan.plan.model;

import java.util.List;

/**
 * One meal in a day: the dish name, its ingredients, and kilocalories.
 */
public record Meal(String name, List<String> ingredients, int kcal) {

    public Meal {
        ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
    }
}
