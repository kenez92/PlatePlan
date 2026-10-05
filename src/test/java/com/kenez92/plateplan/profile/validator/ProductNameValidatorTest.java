package com.kenez92.plateplan.profile.validator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductNameValidatorTest {

    @Test
    void shouldAcceptTwoCharacters() {
        final ProductNameValidator validator = new ProductNameValidator();

        final boolean actual = validator.isValid("ry");

        assertThat(actual).isTrue();
    }

    @Test
    void shouldRejectOneCharacter() {
        final ProductNameValidator validator = new ProductNameValidator();

        final boolean actual = validator.isValid("r");

        assertThat(actual).isFalse();
    }

    @Test
    void shouldAcceptOneHundredCharactersAndRejectOneHundredAndOne() {
        final ProductNameValidator validator = new ProductNameValidator();

        final boolean longest = validator.isValid("a".repeat(100));
        final boolean tooLong = validator.isValid("a".repeat(101));

        assertThat(longest).isTrue();
        assertThat(tooLong).isFalse();
    }

    @Test
    void shouldRejectASemicolon() {
        final ProductNameValidator validator = new ProductNameValidator();

        final boolean actual = validator.isValid("ser;mleko");

        assertThat(actual).isFalse();
    }

    @Test
    void shouldAcceptAComma() {
        final ProductNameValidator validator = new ProductNameValidator();

        final boolean actual = validator.isValid("mleko 3,2%");

        assertThat(actual).isTrue();
    }

    @Test
    void shouldRejectControlFormatAndNonOrdinarySpaceCharacters() {
        final ProductNameValidator validator = new ProductNameValidator();

        final boolean control = validator.isValid("se\u0007r");
        final boolean format = validator.isValid("se\u200br");
        final boolean nonBreakingSpace = validator.isValid("ser\u00a0zolty");

        assertThat(control).isFalse();
        assertThat(format).isFalse();
        assertThat(nonBreakingSpace).isFalse();
    }

    @Test
    void shouldRejectANameThatIsNotAlreadyNormalized() {
        final ProductNameValidator validator = new ProductNameValidator();

        final boolean leadingSpace = validator.isValid(" jajka");
        final boolean trailingSpace = validator.isValid("jajka ");
        final boolean doubleSpace = validator.isValid("ser  zolty");
        final boolean decomposed = validator.isValid("e\u0301gg");

        assertThat(leadingSpace).isFalse();
        assertThat(trailingSpace).isFalse();
        assertThat(doubleSpace).isFalse();
        assertThat(decomposed).isFalse();
    }
}
