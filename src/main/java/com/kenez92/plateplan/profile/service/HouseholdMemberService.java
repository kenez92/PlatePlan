package com.kenez92.plateplan.profile.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.kenez92.plateplan.profile.controller.dto.HouseholdMemberForm;
import com.kenez92.plateplan.profile.db.HouseholdMember;
import com.kenez92.plateplan.profile.db.HouseholdMemberRepository;
import com.kenez92.plateplan.profile.db.UserProfile;
import com.kenez92.plateplan.profile.db.UserProfileRepository;
import com.kenez92.plateplan.profile.format.ProductListFormat;
import com.kenez92.plateplan.profile.model.HouseholdMemberError;
import com.kenez92.plateplan.profile.model.HouseholdMemberResult;
import com.kenez92.plateplan.profile.validator.DisplayNameValidator;
import com.kenez92.plateplan.profile.validator.PersonBoundsValidator;
import com.kenez92.plateplan.profile.validator.ProductListsValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

/**
 * Reads and writes the added people of one account. A save needs the owner's row. The first save
 * stores the formula when it is inside the calorie range and otherwise writes nothing. A later
 * body save keeps the stored calorie target. A calorie correction and a recalculation replace only
 * that target, and only when the number is inside the calorie range; a result outside the range
 * writes nothing and leaves the previous number. At most three added people are stored. Names are
 * unique with the owner's effective name on that login. Removing a row, correcting calories, and
 * recalculating all match the login, so another account's id changes nothing and returns none of
 * its values. Expected outcomes are a {@link HouseholdMemberResult}, not an exception. No message,
 * login, name, or body value is logged. The service is not transactional on purpose: the exceptions
 * must be caught here.
 */
@Service
public class HouseholdMemberService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HouseholdMemberService.class);

    private static final String FAILED_LOG = "Profile storage failed on the database: {} (cause: {})";
    private static final int MAX_MEMBERS = 3;
    private static final int NO_ROW_UPDATED = 0;

    private final UserProfileRepository userProfileRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final ProductListFormat productListFormat;
    private final CalorieService calorieService;
    private final DisplayNameValidator displayNameValidator;
    private final PersonBoundsValidator personBoundsValidator;
    private final ProductListsValidator productListsValidator;

    public HouseholdMemberService(final UserProfileRepository userProfileRepository,
                                  final HouseholdMemberRepository householdMemberRepository,
                                  final ProductListFormat productListFormat,
                                  final CalorieService calorieService,
                                  final DisplayNameValidator displayNameValidator,
                                  final PersonBoundsValidator personBoundsValidator,
                                  final ProductListsValidator productListsValidator) {
        this.userProfileRepository = userProfileRepository;
        this.householdMemberRepository = householdMemberRepository;
        this.productListFormat = productListFormat;
        this.calorieService = calorieService;
        this.displayNameValidator = displayNameValidator;
        this.personBoundsValidator = personBoundsValidator;
        this.productListsValidator = productListsValidator;
    }

    /**
     * The added people of this login, {@code member_id} ascending.
     */
    public HouseholdMemberResult list(final String login) {
        try {
            return HouseholdMemberResult.listed(byMemberId(householdMemberRepository.findByLoginOrderByMemberIdAsc(login)));
        } catch (final DataAccessException | TransactionException exception) {
            logFailure(exception);
            return HouseholdMemberResult.rejected(HouseholdMemberError.UNAVAILABLE);
        }
    }

    /**
     * Inserts one added person when the owner row exists, the limit is not full, the name is free,
     * and the formula is inside the calorie range.
     */
    public HouseholdMemberResult create(final String login, final HouseholdMemberForm form) {
        try {
            final Optional<UserProfile> owner = userProfileRepository.findById(login);
            if (owner.isEmpty()) {
                return HouseholdMemberResult.rejected(HouseholdMemberError.NO_OWNER);
            }
            final HouseholdMemberError invalid = invalid(form);
            if (invalid != null) {
                return HouseholdMemberResult.rejected(invalid);
            }
            final List<HouseholdMember> members = householdMemberRepository.findByLoginOrderByMemberIdAsc(login);
            if (members.size() >= MAX_MEMBERS) {
                return HouseholdMemberResult.rejected(HouseholdMemberError.LIMIT);
            }
            if (conflicts(owner.get(), members, form.displayName(), null)) {
                return HouseholdMemberResult.rejected(HouseholdMemberError.NAME_TAKEN);
            }
            final int calories = formulaCalories(form);
            if (!personBoundsValidator.acceptsCalories(calories)) {
                return HouseholdMemberResult.rejected(HouseholdMemberError.CALORIES);
            }
            return HouseholdMemberResult.saved(householdMemberRepository.save(newMember(login, form, calories)));
        } catch (final DataAccessException | TransactionException exception) {
            logFailure(exception);
            return HouseholdMemberResult.rejected(HouseholdMemberError.UNAVAILABLE);
        }
    }

    /**
     * Replaces the name, body, and product lists of one added person of this login. The calorie
     * target stays. A missing row, including an id of another account, is
     * {@link HouseholdMemberError#NOT_FOUND}.
     */
    public HouseholdMemberResult replaceBody(final String login, final long memberId, final HouseholdMemberForm form) {
        try {
            final Optional<UserProfile> owner = userProfileRepository.findById(login);
            if (owner.isEmpty()) {
                return HouseholdMemberResult.rejected(HouseholdMemberError.NO_OWNER);
            }
            final HouseholdMemberError invalid = invalid(form);
            if (invalid != null) {
                return HouseholdMemberResult.rejected(invalid);
            }
            final List<HouseholdMember> members = householdMemberRepository.findByLoginOrderByMemberIdAsc(login);
            final Optional<HouseholdMember> current = findMember(members, memberId);
            if (current.isEmpty()) {
                return HouseholdMemberResult.rejected(HouseholdMemberError.NOT_FOUND);
            }
            if (conflicts(owner.get(), members, form.displayName(), memberId)) {
                return HouseholdMemberResult.rejected(HouseholdMemberError.NAME_TAKEN);
            }
            final String preferred = productListFormat.join(form.preferredProducts());
            final String excluded = productListFormat.join(form.excludedProducts());
            if (householdMemberRepository.replaceBody(memberId, login, form.displayName(), form.age(), form.heightCm(),
                    form.weightKg(), form.sex(), form.goal(), form.activityLevel(), preferred, excluded)
                    == NO_ROW_UPDATED) {
                return HouseholdMemberResult.rejected(HouseholdMemberError.NOT_FOUND);
            }
            final HouseholdMember row = current.get();
            row.replaceBody(form.displayName(), form.age(), form.heightCm(), form.weightKg(), form.sex(), form.goal(),
                    form.activityLevel(), preferred, excluded);
            return HouseholdMemberResult.saved(row);
        } catch (final DataAccessException | TransactionException exception) {
            logFailure(exception);
            return HouseholdMemberResult.rejected(HouseholdMemberError.UNAVAILABLE);
        }
    }

    /**
     * Replaces only the calorie target of one added person of this login. A number outside the
     * calorie range writes nothing. A missing row, including an id of another account, is
     * {@link HouseholdMemberError#NOT_FOUND} and carries none of that row's values.
     */
    public HouseholdMemberResult updateCalories(final String login, final long memberId, final int dailyCalories) {
        if (!personBoundsValidator.acceptsCalories(dailyCalories)) {
            return HouseholdMemberResult.rejected(HouseholdMemberError.CALORIES);
        }
        try {
            return storeCalories(login, memberId, dailyCalories);
        } catch (final DataAccessException | TransactionException exception) {
            logFailure(exception);
            return HouseholdMemberResult.rejected(HouseholdMemberError.UNAVAILABLE);
        }
    }

    /**
     * Stores the formula for the saved body of one added person of this login. A result outside the
     * calorie range writes nothing and leaves the previous number. A missing row, including an id of
     * another account, is {@link HouseholdMemberError#NOT_FOUND}.
     */
    public HouseholdMemberResult recalculate(final String login, final long memberId) {
        try {
            final Optional<HouseholdMember> current = findMember(membersOf(login), memberId);
            if (current.isEmpty()) {
                return HouseholdMemberResult.rejected(HouseholdMemberError.NOT_FOUND);
            }
            final HouseholdMember row = current.get();
            final int calories = formulaCalories(row);
            if (!personBoundsValidator.acceptsCalories(calories)) {
                return HouseholdMemberResult.rejected(HouseholdMemberError.CALORIES);
            }
            return writeCalories(row, login, calories);
        } catch (final DataAccessException | TransactionException exception) {
            logFailure(exception);
            return HouseholdMemberResult.rejected(HouseholdMemberError.UNAVAILABLE);
        }
    }

    /**
     * Removes one row of this login. An id of another account removes nothing.
     */
    public HouseholdMemberResult delete(final String login, final long memberId) {
        try {
            if (householdMemberRepository.deleteByMemberIdAndLogin(memberId, login) == NO_ROW_UPDATED) {
                return HouseholdMemberResult.rejected(HouseholdMemberError.NOT_FOUND);
            }
            return HouseholdMemberResult.deleted();
        } catch (final DataAccessException | TransactionException exception) {
            logFailure(exception);
            return HouseholdMemberResult.rejected(HouseholdMemberError.UNAVAILABLE);
        }
    }

    private HouseholdMemberError invalid(final HouseholdMemberForm form) {
        if (!personBoundsValidator.acceptsBody(form.age(), form.heightCm(), form.weightKg(), form.sex(), form.goal(),
                form.activityLevel())) {
            return HouseholdMemberError.BODY;
        }
        if (!displayNameValidator.isAcceptable(form.displayName())) {
            return HouseholdMemberError.NAME;
        }
        if (!productListsValidator.accepts(form.preferredProducts(), form.excludedProducts())) {
            return HouseholdMemberError.PRODUCTS;
        }
        return null;
    }

    private boolean conflicts(final UserProfile owner,
                              final List<HouseholdMember> members,
                              final String displayName,
                              final Long exceptMemberId) {
        final String key = displayNameValidator.matchingKey(displayName);
        if (displayNameValidator.matchingKey(owner.getDisplayName()).equals(key)) {
            return true;
        }
        return members.stream()
                .filter(member -> exceptMemberId == null || !exceptMemberId.equals(member.getMemberId()))
                .map(HouseholdMember::getDisplayName)
                .map(displayNameValidator::matchingKey)
                .anyMatch(key::equals);
    }

    private Optional<HouseholdMember> findMember(final List<HouseholdMember> members, final long memberId) {
        return members.stream()
                .filter(member -> member.getMemberId() != null && member.getMemberId() == memberId)
                .findFirst();
    }

    private List<HouseholdMember> byMemberId(final List<HouseholdMember> members) {
        return members.stream()
                .sorted(Comparator.comparing(HouseholdMember::getMemberId))
                .toList();
    }

    private HouseholdMemberResult storeCalories(final String login, final long memberId, final int calories) {
        final Optional<HouseholdMember> current = findMember(membersOf(login), memberId);
        if (current.isEmpty()) {
            return HouseholdMemberResult.rejected(HouseholdMemberError.NOT_FOUND);
        }
        return writeCalories(current.get(), login, calories);
    }

    private HouseholdMemberResult writeCalories(final HouseholdMember row, final String login, final int calories) {
        if (householdMemberRepository.replaceConfirmedCalories(row.getMemberId(), login, calories) == NO_ROW_UPDATED) {
            return HouseholdMemberResult.rejected(HouseholdMemberError.NOT_FOUND);
        }
        row.replaceConfirmedCalories(calories);
        return HouseholdMemberResult.saved(row);
    }

    private List<HouseholdMember> membersOf(final String login) {
        return householdMemberRepository.findByLoginOrderByMemberIdAsc(login);
    }

    private HouseholdMember newMember(final String login, final HouseholdMemberForm form, final int calories) {
        return new HouseholdMember(login, form.displayName(), form.age(), form.heightCm(), form.weightKg(),
                form.sex(), form.goal(), form.activityLevel(), calories,
                productListFormat.join(form.preferredProducts()), productListFormat.join(form.excludedProducts()));
    }

    private int formulaCalories(final HouseholdMemberForm form) {
        return calorieService.dailyCalories(form.age(), form.heightCm(), form.weightKg(), form.sex(),
                form.activityLevel(), form.goal());
    }

    private int formulaCalories(final HouseholdMember member) {
        return calorieService.dailyCalories(member.getAge(), member.getHeightCm(), member.getWeightKg(),
                member.getSex(), member.getActivityLevel(), member.getGoal());
    }

    private void logFailure(final Exception exception) {
        LOGGER.warn(FAILED_LOG, exception.getClass().getName(), causeName(exception));
    }

    private String causeName(final Exception exception) {
        return NestedExceptionUtils.getMostSpecificCause(exception).getClass().getName();
    }
}
