package com.kenez92.plateplan.plan.model;

import java.util.List;
import java.util.Objects;

/**
 * One dish in a day. The name and the preparation are shared. Kilocalories, macros, and ingredients
 * belong to each person's {@link Portion}.
 */
public record Meal(String name, String preparation, List<Portion> portions) {

    private static final String EMPTY = "";

    public Meal {
        name = Objects.requireNonNull(name);
        preparation = preparation == null ? EMPTY : preparation;
        portions = portions == null ? List.of() : List.copyOf(portions);
    }
}
