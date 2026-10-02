package com.kenez92.plateplan.account;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginNormalizerTest {

    @Test
    void shouldRemoveTheWhiteSpaceAroundTheLogin() {
        final LoginNormalizer normalizer = new LoginNormalizer();

        final String actual = normalizer.normalize("  alice \t");

        assertThat(actual).isEqualTo("alice");
    }

    @Test
    void shouldRemoveTheWhiteSpaceOfWiderThanAsciiAroundTheLogin() {
        final LoginNormalizer normalizer = new LoginNormalizer();

        final String actual = normalizer.normalize("\u3000alice\u2003");

        assertThat(actual).isEqualTo("alice");
    }

    @Test
    void shouldComposeTheLoginToNfc() {
        final LoginNormalizer normalizer = new LoginNormalizer();

        final String actual = normalizer.normalize("e\u0301ve");

        assertThat(actual).isEqualTo("\u00e9ve");
    }

    @Test
    void shouldKeepTheLetterCase() {
        final LoginNormalizer normalizer = new LoginNormalizer();

        final String actual = normalizer.normalize("AlIcE");

        assertThat(actual).isEqualTo("AlIcE");
    }

    @Test
    void shouldTurnAMissingLoginIntoAnEmptyOne() {
        final LoginNormalizer normalizer = new LoginNormalizer();

        final String actual = normalizer.normalize(null);

        assertThat(actual).isEmpty();
    }
}
