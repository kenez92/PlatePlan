package com.kenez92.plateplan.profile.validator;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.kenez92.plateplan.profile.controller.dto.ProfileFormDto;
import com.kenez92.plateplan.profile.format.ProductListFormat;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * The product-list rules of {@link ProfileFormDto}. Each list may be empty. The check order inside
 * one list is invalid name, repeat, list full. A name on both lists is a conflict on
 * {@code excludedProducts}, and only when that field has no other problem.
 */
@Component
public class ProductListsValidator implements Validator {

    private static final String PREFERRED_FIELD = "preferredProducts";
    private static final String EXCLUDED_FIELD = "excludedProducts";
    private static final String INVALID = "profile.product.invalid";
    private static final String DUPLICATE = "profile.product.duplicate";
    private static final String FULL = "profile.product.full";
    private static final String CONFLICT = "profile.product.conflict";
    private static final int MAX_PRODUCTS = 50;

    private final ProductNameValidator productNameValidator;
    private final ProductListFormat productListFormat;

    public ProductListsValidator(final ProductNameValidator productNameValidator,
                                 final ProductListFormat productListFormat) {
        this.productNameValidator = productNameValidator;
        this.productListFormat = productListFormat;
    }

    @Override
    public boolean supports(final Class<?> type) {
        return ProfileFormDto.class.isAssignableFrom(type);
    }

    @Override
    public void validate(final Object target, final Errors errors) {
        final ProfileFormDto form = (ProfileFormDto) target;
        final List<String> preferred = productListFormat.split(form.preferredProducts());
        final List<String> excluded = productListFormat.split(form.excludedProducts());
        rejectList(errors, PREFERRED_FIELD, preferred);
        if (rejectList(errors, EXCLUDED_FIELD, excluded)) {
            return;
        }
        final List<String> conflicting = conflicts(preferred, excluded);
        if (!conflicting.isEmpty()) {
            errors.rejectValue(EXCLUDED_FIELD, CONFLICT, conflicting.toArray(), CONFLICT);
        }
    }

    private boolean rejectList(final Errors errors, final String field, final List<String> names) {
        final List<String> invalid = names.stream()
                .filter(name -> !productNameValidator.isValid(name))
                .toList();
        if (!invalid.isEmpty()) {
            errors.rejectValue(field, INVALID, invalid.toArray(), INVALID);
            return true;
        }
        final List<String> repeated = findRepeated(names);
        if (!repeated.isEmpty()) {
            errors.rejectValue(field, DUPLICATE, repeated.toArray(), DUPLICATE);
            return true;
        }
        if (names.size() > MAX_PRODUCTS) {
            errors.rejectValue(field, FULL, new Object[0], FULL);
            return true;
        }
        return false;
    }

    private List<String> conflicts(final List<String> preferred, final List<String> excluded) {
        final Set<String> preferredKeys = preferred.stream()
                .map(ProductListsValidator::sameProductKey)
                .collect(Collectors.toSet());
        return excluded.stream()
                .filter(name -> preferredKeys.contains(sameProductKey(name)))
                .toList();
    }

    private List<String> findRepeated(final List<String> names) {
        final Set<String> seen = new HashSet<>();
        return names.stream()
                .filter(name -> !seen.add(sameProductKey(name)))
                .distinct()
                .toList();
    }

    private static String sameProductKey(final String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
