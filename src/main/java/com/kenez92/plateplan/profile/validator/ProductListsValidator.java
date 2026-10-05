package com.kenez92.plateplan.profile.validator;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.kenez92.plateplan.profile.controller.dto.ProfileFormDto;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * The product-list rules of {@link ProfileFormDto}. Each list may be empty. Uniqueness is already
 * on the form. The check order inside one list is invalid name, then list full. A name on both
 * lists is a conflict on {@code excludedProducts}, and only when that field has no other problem.
 */
@Component
public class ProductListsValidator implements Validator {

    private static final String PREFERRED_FIELD = "preferredProducts";
    private static final String EXCLUDED_FIELD = "excludedProducts";
    private static final String INVALID = "profile.product.invalid";
    private static final String FULL = "profile.product.full";
    private static final String CONFLICT = "profile.product.conflict";
    private static final int MAX_PRODUCTS = 50;
    private static final Object[] NO_MESSAGE_ARGUMENTS = new Object[0];
    private static final String JOINED_NAMES_SEPARATOR = ", ";

    private final ProductNameValidator productNameValidator;

    public ProductListsValidator(final ProductNameValidator productNameValidator) {
        this.productNameValidator = productNameValidator;
    }

    @Override
    public boolean supports(final Class<?> type) {
        return ProfileFormDto.class.isAssignableFrom(type);
    }

    @Override
    public void validate(final Object target, final Errors errors) {
        final ProfileFormDto form = (ProfileFormDto) target;
        final List<String> preferred = form.preferredProducts();
        final List<String> excluded = form.excludedProducts();
        rejectList(errors, PREFERRED_FIELD, preferred);
        if (rejectList(errors, EXCLUDED_FIELD, excluded)) {
            return;
        }
        final List<String> conflicting = conflicts(preferred, excluded);
        if (!conflicting.isEmpty()) {
            errors.rejectValue(EXCLUDED_FIELD, CONFLICT, namesArgument(conflicting), CONFLICT);
        }
    }

    private boolean rejectList(final Errors errors, final String field, final List<String> names) {
        final List<String> invalid = names.stream()
                .filter(name -> !productNameValidator.isValid(name))
                .toList();
        if (!invalid.isEmpty()) {
            errors.rejectValue(field, INVALID, namesArgument(invalid), INVALID);
            return true;
        }
        if (names.size() > MAX_PRODUCTS) {
            errors.rejectValue(field, FULL, NO_MESSAGE_ARGUMENTS, FULL);
            return true;
        }
        return false;
    }

    private Object[] namesArgument(final List<String> names) {
        return new Object[] {String.join(JOINED_NAMES_SEPARATOR, names)};
    }

    private List<String> conflicts(final List<String> preferred, final List<String> excluded) {
        final Set<String> preferredKeys = preferred.stream()
                .map(ProductListsValidator::sameProductKey)
                .collect(Collectors.toSet());
        return excluded.stream()
                .filter(name -> preferredKeys.contains(sameProductKey(name)))
                .toList();
    }

    private static String sameProductKey(final String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
