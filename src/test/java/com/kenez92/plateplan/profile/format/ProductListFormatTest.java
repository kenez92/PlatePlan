package com.kenez92.plateplan.profile.format;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductListFormatTest {

    @Test
    void shouldSplitOnSemicolonsAndKeepCommasInsideAName() {
        final List<String> actual = format().split("mleko 3,2%;jajka;ser");

        assertThat(actual).usingRecursiveComparison().isEqualTo(List.of("mleko 3,2%", "jajka", "ser"));
    }

    @Test
    void shouldKeepEmptyPartsFromADoubleOrTrailingSemicolon() {
        final List<String> actual = format().split(";jajka;;ser;");

        assertThat(actual).usingRecursiveComparison().isEqualTo(List.of("", "jajka", "", "ser", ""));
    }

    @Test
    void shouldTreatNullOrEmptyAsEmpty() {
        final ProductListFormat format = format();

        assertThat(format.split(null)).isEmpty();
        assertThat(format.split("")).isEmpty();
    }

    @Test
    void shouldStoreNullWhenTheListIsEmpty() {
        assertThat(format().join(List.of())).isNull();
    }

    @Test
    void shouldJoinWithSemicolonsAndNoSpaces() {
        final String actual = format().join(List.of("mleko 3,2%", "jajka", "ser"));

        assertThat(actual).isEqualTo("mleko 3,2%;jajka;ser");
    }

    @Test
    void shouldReturnTheSameListAfterJoinAndSplit() {
        final ProductListFormat format = format();
        final List<String> products = List.of("mleko 3,2%", "Jajka", "ser \u017c\u00f3\u0142ty");

        final List<String> actual = format.split(format.join(products));

        assertThat(actual).usingRecursiveComparison().isEqualTo(products);
    }

    private ProductListFormat format() {
        return new ProductListFormat();
    }
}
