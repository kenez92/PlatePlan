package com.kenez92.plateplan.plan.model;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ShoppingItemTest {

    @Test
    void shouldReadAShoppingItemWhenTheCategoryIsMissing() {
        final JsonMapper mapper = JsonMapper.builder().build();

        final ShoppingItem actual = mapper.readValue(
                "{\"name\":\"sałata masłowa\",\"amount\":\"300 g\"}", ShoppingItem.class);

        final ShoppingItem expected = new ShoppingItem("sałata masłowa", "300 g", "");
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReadAShoppingItemWhenTheCategoryIsNull() {
        final JsonMapper mapper = JsonMapper.builder().build();

        final ShoppingItem actual = mapper.readValue(
                "{\"name\":\"masło\",\"amount\":\"40 g\",\"category\":null}", ShoppingItem.class);

        final ShoppingItem expected = new ShoppingItem("masło", "40 g", "");
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
}
