package com.kenez92.plateplan.profile.validator;

import java.math.BigDecimal;
import java.util.List;

import com.kenez92.plateplan.profile.controller.dto.ProfileFormDto;
import com.kenez92.plateplan.profile.model.ActivityLevel;
import com.kenez92.plateplan.profile.model.Goal;
import com.kenez92.plateplan.profile.model.Sex;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;

import static org.assertj.core.api.Assertions.assertThat;

class EmailValidatorTest {

    @Test
    void shouldAcceptAMissingEmail() {
        final EmailValidator validator = new EmailValidator();

        assertThat(validator.isValid(null)).isTrue();
        assertThat(errorsFor(null).hasFieldErrors("email")).isFalse();
    }

    @Test
    void shouldAcceptASimpleAddress() {
        final EmailValidator validator = new EmailValidator();

        assertThat(validator.isValid("alice@example.com")).isTrue();
    }

    @Test
    void shouldRefuseAnAddressWithoutAt() {
        final EmailValidator validator = new EmailValidator();

        assertThat(validator.isValid("alice.example.com")).isFalse();
        assertThat(errorsFor("alice.example.com").hasFieldErrors("email")).isTrue();
    }

    @Test
    void shouldRefuseAControlCharacter() {
        final EmailValidator validator = new EmailValidator();

        assertThat(validator.isValid("alice@example.com\n")).isFalse();
    }

    private BeanPropertyBindingResult errorsFor(final String email) {
        final ProfileFormDto form = new ProfileFormDto(34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN,
                ActivityLevel.MODERATE, List.of(), List.of(), email);
        final BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "profileForm");
        new EmailValidator().validate(form, errors);
        return errors;
    }
}
