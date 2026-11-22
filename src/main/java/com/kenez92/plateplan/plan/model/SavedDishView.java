package com.kenez92.plateplan.plan.model;

import java.util.Objects;

/**
 * One saved dish on the plan screen. {@code opinion} is null until the account marks it. The name
 * and the preparation are personal, so {@link #toString()} shows none of them.
 */
public record SavedDishView(long dishId, String name, String preparation, DishOpinion opinion) {

    private static final String REDACTED = "SavedDishView[redacted]";
    private static final String EMPTY = "";

    public SavedDishView {
        name = Objects.requireNonNull(name);
        preparation = preparation == null ? EMPTY : preparation;
    }

    public boolean liked() {
        return opinion == DishOpinion.LIKE;
    }

    public boolean disliked() {
        return opinion == DishOpinion.DISLIKE;
    }

    @Override
    public String toString() {
        return REDACTED;
    }
}
