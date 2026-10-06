package com.kenez92.plateplan.plan.model;

import java.util.Objects;

/**
 * One line on the day's shopping list: what to buy and how much.
 */
public record ShoppingItem(String name, String amount) {

    public ShoppingItem {
        name = Objects.requireNonNull(name);
        amount = amount == null ? "" : amount;
    }
}
