package com.kenez92.plateplan.plan.model;

import java.util.List;
import java.util.Objects;

/**
 * One person's share of a shared dish: the effective name, the ingredients of that share, the
 * kilocalories, and the macros in whole grams.
 */
public record Portion(String personName,
                      List<Ingredient> ingredients,
                      int kcal,
                      int proteinG,
                      int carbsG,
                      int fatG) {

    public Portion {
        personName = Objects.requireNonNull(personName);
        ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
    }
}
