package com.kenez92.plateplan.plan.model;

import java.util.Objects;

/**
 * One line on the day's shopping list: what to buy and how much.
 */
public record ShoppingItem(String name, String amount) {

    private static final String EMPTY = "";

    public ShoppingItem {
        name = Objects.requireNonNull(name);
        amount = amount == null ? EMPTY : amount;
    }
}
