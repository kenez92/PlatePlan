package com.kenez92.plateplan.plan.model;

import java.util.Objects;

/**
 * One food in a meal: the name and how much of it the dish uses.
 */
public record Ingredient(String name, String amount) {

    public Ingredient {
        name = Objects.requireNonNull(name);
        amount = amount == null ? "" : amount;
    }
}
