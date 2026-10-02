package com.kenez92.plateplan.account;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationValidatorTest {

    @Test
    void shouldAcceptAValidLoginAndPassword() {
        final RegistrationValidator validator = new RegistrationValidator();

        final Optional<RegistrationError> actual = validator.validate("alice", "correct horse");

        assertThat(actual).isEmpty();
    }

    @Test
    void shouldAcceptALoginOfThreeAndOfFiftyCharacters() {
        final RegistrationValidator validator = new RegistrationValidator();

        final Optional<RegistrationError> shortest = validator.validate("abc", "correct horse");
        final Optional<RegistrationError> longest = validator.validate("a".repeat(50), "correct horse");

        assertThat(shortest).isEmpty();
        assertThat(longest).isEmpty();
    }

    @Test
    void shouldRejectALoginShorterThanThreeCharacters() {
        final RegistrationValidator validator = new RegistrationValidator();

        final Optional<RegistrationError> actual = validator.validate("ab", "correct horse");

        assertThat(actual).usingRecursiveComparison().isEqualTo(Optional.of(RegistrationError.LOGIN_INVALID));
    }

    @Test
    void shouldRejectALoginLongerThanFiftyCharacters() {
        final RegistrationValidator validator = new RegistrationValidator();

        final Optional<RegistrationError> actual = validator.validate("a".repeat(51), "correct horse");

        assertThat(actual).usingRecursiveComparison().isEqualTo(Optional.of(RegistrationError.LOGIN_INVALID));
    }

    @Test
    void shouldRejectAMissingLogin() {
        final RegistrationValidator validator = new RegistrationValidator();

        final Optional<RegistrationError> actual = validator.validate(null, "correct horse");

        assertThat(actual).usingRecursiveComparison().isEqualTo(Optional.of(RegistrationError.LOGIN_INVALID));
    }

    @Test
    void shouldRejectAPasswordShorterThanEightCharacters() {
        final RegistrationValidator validator = new RegistrationValidator();

        final Optional<RegistrationError> actual = validator.validate("alice", "1234567");

        assertThat(actual).usingRecursiveComparison().isEqualTo(Optional.of(RegistrationError.PASSWORD_INVALID));
    }

    @Test
    void shouldRejectAPasswordLongerThan72Bytes() {
        final RegistrationValidator validator = new RegistrationValidator();

        final Optional<RegistrationError> actual = validator.validate("alice", "ą".repeat(37));

        assertThat(actual).usingRecursiveComparison().isEqualTo(Optional.of(RegistrationError.PASSWORD_INVALID));
    }

    @Test
    void shouldAcceptAPasswordOf72BytesExactly() {
        final RegistrationValidator validator = new RegistrationValidator();

        final Optional<RegistrationError> actual = validator.validate("alice", "ą".repeat(36));

        assertThat(actual).isEmpty();
    }

    @Test
    void shouldRejectAMissingPassword() {
        final RegistrationValidator validator = new RegistrationValidator();

        final Optional<RegistrationError> actual = validator.validate("alice", null);

        assertThat(actual).usingRecursiveComparison().isEqualTo(Optional.of(RegistrationError.PASSWORD_INVALID));
    }
}
