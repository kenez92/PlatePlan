package com.kenez92.plateplan.plan.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One line on the shopping list: what to buy, how much, and which shop shelf it belongs on.
 * A missing shelf is empty so an older list still loads.
 */
public record ShoppingItem(String name, String amount, @JsonProperty(required = false) String category) {

    private static final String EMPTY = "";

    @JsonCreator
    public ShoppingItem {
        name = Objects.requireNonNull(name);
        amount = amount == null ? EMPTY : amount;
        category = category == null ? EMPTY : category;
    }

    public ShoppingItem(final String name, final String amount) {
        this(name, amount, EMPTY);
    }
}
