package com.kenez92.plateplan.profile.validator;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.kenez92.plateplan.profile.controller.dto.ProfileFormDto;
import com.kenez92.plateplan.profile.model.ActivityLevel;
import com.kenez92.plateplan.profile.model.Goal;
import com.kenez92.plateplan.profile.format.ProductListFormat;
import com.kenez92.plateplan.profile.model.Sex;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

import static org.assertj.core.api.Assertions.assertThat;

class ProductListsValidatorTest {

    @Test
    void shouldAcceptEmptyProductLists() {
        final BeanPropertyBindingResult errors = errors(form("", null));

        validator().validate(form("", null), errors);

        assertThat(errors.hasErrors()).isFalse();
    }

    @Test
    void shouldRefuseANameWithALeadingSpaceAfterASemicolon() {
        final ProfileFormDto form = form("jajka; ser", "orzechy");
        final BeanPropertyBindingResult errors = errors(form);

        validator().validate(form, errors);

        assertThat(fieldErrors(errors)).usingRecursiveComparison().isEqualTo(List.of(
                new Violation("preferredProducts", "profile.product.invalid", List.of(" ser"))));
    }

    @Test
    void shouldRefuseAnInvalidProductName() {
        final ProfileFormDto form = form("ser;a;b", "orzechy");
        final BeanPropertyBindingResult errors = errors(form);

        validator().validate(form, errors);

        assertThat(fieldErrors(errors)).usingRecursiveComparison().isEqualTo(List.of(
                new Violation("preferredProducts", "profile.product.invalid", List.of("a", "b"))));
    }

    @Test
    void shouldRefuseARepeatInsideOneListIgnoringCase() {
        final ProfileFormDto form = form("Mleko;mleko", "orzechy");
        final BeanPropertyBindingResult errors = errors(form);

        validator().validate(form, errors);

        assertThat(fieldErrors(errors)).usingRecursiveComparison().isEqualTo(List.of(
                new Violation("preferredProducts", "profile.product.duplicate", List.of("mleko"))));
    }

    @Test
    void shouldAcceptFiftyProductsAndRefuseTheFiftyFirst() {
        final ProductListsValidator validator = validator();
        final ProfileFormDto fifty = form(products(50), "orzechy");
        final ProfileFormDto fiftyOne = form(products(51), "orzechy");
        final BeanPropertyBindingResult accepted = errors(fifty);
        final BeanPropertyBindingResult refused = errors(fiftyOne);

        validator.validate(fifty, accepted);
        validator.validate(fiftyOne, refused);

        assertThat(accepted.hasErrors()).isFalse();
        assertThat(fieldErrors(refused)).usingRecursiveComparison().isEqualTo(List.of(
                new Violation("preferredProducts", "profile.product.full", List.of())));
    }

    @Test
    void shouldRefuseAProductOnBothListsAndReportItOnTheExcludedField() {
        final ProfileFormDto form = form("ser;jajka", "orzechy;Ser");
        final BeanPropertyBindingResult errors = errors(form);

        validator().validate(form, errors);

        assertThat(fieldErrors(errors)).usingRecursiveComparison().isEqualTo(List.of(
                new Violation("excludedProducts", "profile.product.conflict", List.of("Ser"))));
    }

    @Test
    void shouldReportAConflictOnlyWhenTheExcludedFieldHasNoOtherProblem() {
        final ProfileFormDto form = form("ser", "ser;ser");
        final BeanPropertyBindingResult errors = errors(form);

        validator().validate(form, errors);

        assertThat(fieldErrors(errors)).usingRecursiveComparison().isEqualTo(List.of(
                new Violation("excludedProducts", "profile.product.duplicate", List.of("ser"))));
    }

    private ProductListsValidator validator() {
        return new ProductListsValidator(new ProductNameValidator(), new ProductListFormat());
    }

    private BeanPropertyBindingResult errors(final ProfileFormDto form) {
        return new BeanPropertyBindingResult(form, "profileForm");
    }

    private List<Violation> fieldErrors(final BeanPropertyBindingResult errors) {
        return errors.getFieldErrors().stream()
                .map(error -> new Violation(error.getField(), error.getCode(), arguments(error)))
                .sorted(Comparator.comparing(Violation::field).thenComparing(Violation::code))
                .toList();
    }

    private List<String> arguments(final FieldError error) {
        if (error.getArguments() == null) {
            return List.of();
        }
        return Arrays.stream(error.getArguments())
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }

    private String products(final int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(index -> "produkt " + index)
                .collect(Collectors.joining(";"));
    }

    private ProfileFormDto form(final String preferredProducts, final String excludedProducts) {
        return new ProfileFormDto(34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN, ActivityLevel.MODERATE,
                preferredProducts, excludedProducts);
    }

    private record Violation(String field, String code, List<String> products) {
    }
}
