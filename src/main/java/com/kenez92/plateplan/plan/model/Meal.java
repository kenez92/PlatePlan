package com.kenez92.plateplan.plan.model;

import java.util.List;
import java.util.Objects;

/**
 * One meal in a day: the dish name, ingredients, how to prepare it, kilocalories, and macros in
 * whole grams.
 */
public record Meal(String name,
                   List<Ingredient> ingredients,
                   int kcal,
                   int proteinG,
                   int carbsG,
                   int fatG,
                   String preparation) {

    public Meal {
        name = Objects.requireNonNull(name);
        ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
        preparation = preparation == null ? "" : preparation;
    }
}
