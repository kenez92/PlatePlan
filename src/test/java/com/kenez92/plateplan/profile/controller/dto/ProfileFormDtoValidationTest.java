package com.kenez92.plateplan.profile.controller.dto;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.kenez92.plateplan.profile.model.ActivityLevel;
import com.kenez92.plateplan.profile.model.Goal;
import com.kenez92.plateplan.profile.model.Sex;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileFormDtoValidationTest {

    @Test
    void shouldAcceptAValidProfile() {
        final Set<ConstraintViolation<ProfileFormDto>> actual = validator().validate(valid());

        assertThat(actual).isEmpty();
    }

    @Test
    void shouldRequireEveryBodyField() {
        final Validator validator = validator();
        final Map<String, ProfileFormDto> missing = Map.of(
                "age", new ProfileFormDto(null, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN,
                        ActivityLevel.MODERATE, List.of("mleko"), List.of("orzechy")),
                "heightCm", new ProfileFormDto(34, null, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN,
                        ActivityLevel.MODERATE, List.of("mleko"), List.of("orzechy")),
                "weightKg", new ProfileFormDto(34, 180, null, Sex.MALE, Goal.MAINTAIN, ActivityLevel.MODERATE,
                        List.of("mleko"), List.of("orzechy")),
                "sex", new ProfileFormDto(34, 180, new BigDecimal("82.5"), null, Goal.MAINTAIN,
                        ActivityLevel.MODERATE, List.of("mleko"), List.of("orzechy")),
                "goal", new ProfileFormDto(34, 180, new BigDecimal("82.5"), Sex.MALE, null, ActivityLevel.MODERATE,
                        List.of("mleko"), List.of("orzechy")),
                "activityLevel", new ProfileFormDto(34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN, null,
                        List.of("mleko"), List.of("orzechy")));

        for (final Map.Entry<String, ProfileFormDto> entry : missing.entrySet()) {
            final List<Violation> expected = List.of(new Violation(entry.getKey(), "{profile.required}"));
            assertThat(violations(validator.validate(entry.getValue()))).usingRecursiveComparison().isEqualTo(expected);
        }
    }

    @Test
    void shouldAcceptAgeFrom10To110AndRefuseTheNeighbours() {
        final Validator validator = validator();

        assertThat(validator.validate(withAge(10))).isEmpty();
        assertThat(validator.validate(withAge(110))).isEmpty();
        assertThat(violations(validator.validate(withAge(9)))).usingRecursiveComparison()
                .isEqualTo(List.of(new Violation("age", "{profile.age.invalid}")));
        assertThat(violations(validator.validate(withAge(111)))).usingRecursiveComparison()
                .isEqualTo(List.of(new Violation("age", "{profile.age.invalid}")));
    }

    @Test
    void shouldAcceptHeightFrom80To250AndRefuseTheNeighbours() {
        final Validator validator = validator();

        assertThat(validator.validate(withHeight(80))).isEmpty();
        assertThat(validator.validate(withHeight(250))).isEmpty();
        assertThat(violations(validator.validate(withHeight(79)))).usingRecursiveComparison()
                .isEqualTo(List.of(new Violation("heightCm", "{profile.height.invalid}")));
        assertThat(violations(validator.validate(withHeight(251)))).usingRecursiveComparison()
                .isEqualTo(List.of(new Violation("heightCm", "{profile.height.invalid}")));
    }

    @Test
    void shouldAcceptWeightFrom20To400AndRefuseTheNeighbours() {
        final Validator validator = validator();

        assertThat(validator.validate(withWeight(new BigDecimal("20.0")))).isEmpty();
        assertThat(validator.validate(withWeight(new BigDecimal("400.0")))).isEmpty();
        assertThat(violations(validator.validate(withWeight(new BigDecimal("19.9"))))).usingRecursiveComparison()
                .isEqualTo(List.of(new Violation("weightKg", "{profile.weight.invalid}")));
        assertThat(violations(validator.validate(withWeight(new BigDecimal("400.1"))))).usingRecursiveComparison()
                .isEqualTo(List.of(new Violation("weightKg", "{profile.weight.invalid}")));
    }

    @Test
    void shouldRefuseAWeightWithTwoDecimalPlaces() {
        final List<Violation> actual = violations(validator().validate(withWeight(new BigDecimal("72.55"))));

        assertThat(actual).usingRecursiveComparison()
                .isEqualTo(List.of(new Violation("weightKg", "{profile.weight.invalid}")));
    }

    @Test
    void shouldAcceptEmptyProductLists() {
        final Set<ConstraintViolation<ProfileFormDto>> actual = validator().validate(new ProfileFormDto(34, 180,
                new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN, ActivityLevel.MODERATE, List.of(), null));

        assertThat(actual).isEmpty();
    }

    private Validator validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }

    private List<Violation> violations(final Set<ConstraintViolation<ProfileFormDto>> actual) {
        return actual.stream()
                .map(violation -> new Violation(violation.getPropertyPath().toString(), violation.getMessageTemplate()))
                .sorted(Comparator.comparing(Violation::field).thenComparing(Violation::messageTemplate))
                .toList();
    }

    private ProfileFormDto valid() {
        return new ProfileFormDto(34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN, ActivityLevel.MODERATE,
                List.of("mleko 3,2%", "jajka", "ser"), List.of("orzechy"));
    }

    private ProfileFormDto withAge(final Integer age) {
        return new ProfileFormDto(age, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN, ActivityLevel.MODERATE,
                List.of("mleko"), List.of("orzechy"));
    }

    private ProfileFormDto withHeight(final Integer heightCm) {
        return new ProfileFormDto(34, heightCm, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN, ActivityLevel.MODERATE,
                List.of("mleko"), List.of("orzechy"));
    }

    private ProfileFormDto withWeight(final BigDecimal weightKg) {
        return new ProfileFormDto(34, 180, weightKg, Sex.MALE, Goal.MAINTAIN, ActivityLevel.MODERATE, List.of("mleko"),
                List.of("orzechy"));
    }

    private record Violation(String field, String messageTemplate) {
    }
}
