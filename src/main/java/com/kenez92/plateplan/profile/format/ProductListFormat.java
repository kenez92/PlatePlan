package com.kenez92.plateplan.profile.format;

import java.util.List;

import org.springframework.stereotype.Component;

/**
 * The stored text of a product list: names joined with a semicolon and no spaces, so a comma can be
 * part of a name. An empty list is stored as null. The client sends the same non-empty form; this
 * class does not strip, compose, or drop names.
 */
@Component
public class ProductListFormat {

    private static final String SEPARATOR = ";";
    private static final int KEEP_EMPTY_PARTS = -1;

    public List<String> split(final String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        return List.of(text.split(SEPARATOR, KEEP_EMPTY_PARTS));
    }

    public String join(final List<String> products) {
        if (products.isEmpty()) {
            return null;
        }
        return String.join(SEPARATOR, products);
    }
}
