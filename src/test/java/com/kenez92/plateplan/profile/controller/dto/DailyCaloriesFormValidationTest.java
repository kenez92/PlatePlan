package com.kenez92.plateplan.profile.controller.dto;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DailyCaloriesFormValidationTest {

    @Test
    void shouldAcceptDailyCaloriesFrom800To6000() {
        final Validator validator = validator();

        assertThat(validator.validate(new DailyCaloriesForm(800))).isEmpty();
        assertThat(validator.validate(new DailyCaloriesForm(6000))).isEmpty();
        assertThat(validator.validate(new DailyCaloriesForm(2000))).isEmpty();
    }

    @Test
    void shouldRequireDailyCalories() {
        final List<Violation> actual = violations(validator().validate(DailyCaloriesForm.empty()));

        assertThat(actual).usingRecursiveComparison()
                .isEqualTo(List.of(new Violation("dailyCalories", "{profile.required}")));
    }

    @Test
    void shouldRefuseDailyCaloriesOutsideTheRange() {
        final Validator validator = validator();

        assertThat(violations(validator.validate(new DailyCaloriesForm(799)))).usingRecursiveComparison()
                .isEqualTo(List.of(new Violation("dailyCalories", "{profile.calories.invalid}")));
        assertThat(violations(validator.validate(new DailyCaloriesForm(6001)))).usingRecursiveComparison()
                .isEqualTo(List.of(new Violation("dailyCalories", "{profile.calories.invalid}")));
    }

    private Validator validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }

    private List<Violation> violations(final Set<ConstraintViolation<DailyCaloriesForm>> actual) {
        return actual.stream()
                .map(violation -> new Violation(violation.getPropertyPath().toString(), violation.getMessageTemplate()))
                .sorted(Comparator.comparing(Violation::field).thenComparing(Violation::messageTemplate))
                .toList();
    }

    private record Violation(String field, String messageTemplate) {
    }
}
