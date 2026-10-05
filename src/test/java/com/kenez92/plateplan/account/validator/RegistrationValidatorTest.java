package com.kenez92.plateplan.account.validator;

import java.util.Optional;

import com.kenez92.plateplan.account.model.RegistrationError;
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

    @Test
    void shouldAcceptAnOrdinarySpaceInsideTheLogin() {
        final RegistrationValidator validator = new RegistrationValidator();

        final Optional<RegistrationError> actual = validator.validate("ann marie", "correct horse");

        assertThat(actual).isEmpty();
    }

    @Test
    void shouldRejectALoginWithAControlCharacter() {
        final RegistrationValidator validator = new RegistrationValidator();

        final Optional<RegistrationError> actual = validator.validate("ab\u0007c", "correct horse");

        assertThat(actual).usingRecursiveComparison().isEqualTo(Optional.of(RegistrationError.LOGIN_INVALID));
    }

    @Test
    void shouldRejectALoginWithAZeroWidthCharacter() {
        final RegistrationValidator validator = new RegistrationValidator();

        final Optional<RegistrationError> actual = validator.validate("ab\u200bc", "correct horse");

        assertThat(actual).usingRecursiveComparison().isEqualTo(Optional.of(RegistrationError.LOGIN_INVALID));
    }

    @Test
    void shouldRejectALoginWithANonBreakingSpace() {
        final RegistrationValidator validator = new RegistrationValidator();

        final Optional<RegistrationError> inside = validator.validate("ab\u00a0c", "correct horse");
        final Optional<RegistrationError> outside = validator.validate("\u00a0abc", "correct horse");

        assertThat(inside).usingRecursiveComparison().isEqualTo(Optional.of(RegistrationError.LOGIN_INVALID));
        assertThat(outside).usingRecursiveComparison().isEqualTo(Optional.of(RegistrationError.LOGIN_INVALID));
    }

    @Test
    void shouldCountASurrogatePairAsOneCharacterInALogin() {
        final RegistrationValidator validator = new RegistrationValidator();

        final Optional<RegistrationError> threeCharacters = validator.validate("\ud83d\ude00".repeat(3), "correct horse");
        final Optional<RegistrationError> fiftyOneCharacters = validator.validate("\ud83d\ude00".repeat(51), "correct horse");

        assertThat(threeCharacters).isEmpty();
        assertThat(fiftyOneCharacters).usingRecursiveComparison().isEqualTo(Optional.of(RegistrationError.LOGIN_INVALID));
    }

    @Test
    void shouldCountASurrogatePairAsOneCharacterAndFourBytesInAPassword() {
        final RegistrationValidator validator = new RegistrationValidator();

        final Optional<RegistrationError> eightCharacters = validator.validate("alice", "\ud83d\ude00".repeat(8));
        final Optional<RegistrationError> nineteenCharacters = validator.validate("alice", "\ud83d\ude00".repeat(19));

        assertThat(eightCharacters).isEmpty();
        assertThat(nineteenCharacters).usingRecursiveComparison().isEqualTo(Optional.of(RegistrationError.PASSWORD_INVALID));
    }
}