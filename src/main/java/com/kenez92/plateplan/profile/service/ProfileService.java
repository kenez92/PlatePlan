package com.kenez92.plateplan.profile.service;

import java.math.RoundingMode;
import java.util.Optional;

import com.kenez92.plateplan.profile.controller.dto.ProfileFormDto;
import com.kenez92.plateplan.profile.db.HouseholdMemberRepository;
import com.kenez92.plateplan.profile.db.UserProfile;
import com.kenez92.plateplan.profile.db.UserProfileRepository;
import com.kenez92.plateplan.profile.format.ProductListFormat;
import com.kenez92.plateplan.profile.model.ProductLists;
import com.kenez92.plateplan.profile.model.ProfileDetails;
import com.kenez92.plateplan.profile.model.ProfileRefusal;
import com.kenez92.plateplan.profile.model.ProfileResult;
import com.kenez92.plateplan.profile.validator.DisplayNameValidator;
import com.kenez92.plateplan.profile.validator.PersonBoundsValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

/**
 * Reads and writes the profile body, display name, and product lists. It queries the database only
 * when a request arrives. A profile save never takes a calorie number from the client: the first
 * save stores the formula when that result is inside the calorie range, and otherwise writes
 * nothing; a later save keeps the stored target. A body or display name outside its rule writes
 * nothing. The display name is unique with the added people of this login, and a blank name is
 * null. Client edits and recalculation of {@code confirmed_calories} are
 * {@link ConfirmedCaloriesService}. Expected outcomes are returned as a {@link ProfileResult}, not
 * thrown. The profile is personal data and a unique-index violation carries the login in its
 * message, so no message, login, name, or value is logged or passed on; only the class names of
 * the exception and its cause are logged. An existing row is replaced in one statement, so a later
 * body save never writes calories in a second update. Zero rows updated is a failed save. The
 * service is not transactional on purpose: the exceptions must be caught here, and a failure at
 * commit would escape a transactional proxy.
 */
@Service
public class ProfileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileService.class);

    private static final String FAILED_LOG = "Profile storage failed on the database: {} (cause: {})";
    private static final String NO_ROW_LOG = "Profile storage failed: no row updated";
    private static final int WEIGHT_SCALE = 1;
    private static final int NO_ROW_UPDATED = 0;

    private final UserProfileRepository userProfileRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final ProductListFormat productListFormat;
    private final CalorieService calorieService;
    private final DisplayNameValidator displayNameValidator;
    private final PersonBoundsValidator personBoundsValidator;

    public ProfileService(final UserProfileRepository userProfileRepository,
                          final HouseholdMemberRepository householdMemberRepository,
                          final ProductListFormat productListFormat,
                          final CalorieService calorieService,
                          final DisplayNameValidator displayNameValidator,
                          final PersonBoundsValidator personBoundsValidator) {
        this.userProfileRepository = userProfileRepository;
        this.householdMemberRepository = householdMemberRepository;
        this.productListFormat = productListFormat;
        this.calorieService = calorieService;
        this.displayNameValidator = displayNameValidator;
        this.personBoundsValidator = personBoundsValidator;
    }

    public ProfileResult load(final String login) {
        try {
            return userProfileRepository.findById(login)
                    .map(this::toForm)
                    .map(ProfileResult::loaded)
                    .orElseGet(() -> ProfileResult.loaded(ProfileFormDto.empty()));
        } catch (final DataAccessException | TransactionException exception) {
            logFailure(exception);
            return ProfileResult.unavailable(ProfileFormDto.empty());
        }
    }

    public ProfileResult save(final String login, final ProfileFormDto form) {
        if (!personBoundsValidator.acceptsBody(form.age(), form.heightCm(), form.weightKg(), form.sex(), form.goal(),
                form.activityLevel())) {
            return ProfileResult.refused(form, ProfileRefusal.BODY);
        }
        if (form.displayName() != null && !displayNameValidator.isAcceptable(form.displayName())) {
            return ProfileResult.refused(form, ProfileRefusal.NAME);
        }
        try {
            return write(login, toDetails(form), form);
        } catch (final DataAccessException | TransactionException exception) {
            logFailure(exception);
            return ProfileResult.unavailable(form);
        }
    }

    /**
     * True when the stored row has a delivery address. A missing row or a database failure is
     * false, so {@code GET /plan} can hide the mail checkbox without a 500.
     */
    public boolean hasDeliveryEmail(final String login) {
        final ProfileResult result = load(login);
        return result.isSuccessful() && result.profile().email() != null;
    }

    private ProfileDetails toDetails(final ProfileFormDto form) {
        return new ProfileDetails(
                form.age(),
                form.heightCm(),
                form.weightKg(),
                form.sex(),
                form.goal(),
                form.activityLevel(),
                new ProductLists(form.preferredProducts(), form.excludedProducts()),
                form.email(),
                form.displayName());
    }

    private ProfileResult write(final String login, final ProfileDetails details, final ProfileFormDto form) {
        final String preferred = productListFormat.join(details.products().preferred());
        final String excluded = productListFormat.join(details.products().excluded());
        final Optional<UserProfile> existing = userProfileRepository.findById(login);
        if (existing.isEmpty()) {
            return create(login, details, form, preferred, excluded);
        }
        return update(login, details, form, existing.get(), preferred, excluded);
    }

    private ProfileResult create(final String login,
                                 final ProfileDetails details,
                                 final ProfileFormDto form,
                                 final String preferred,
                                 final String excluded) {
        final int calories = formulaCalories(details);
        if (!personBoundsValidator.acceptsCalories(calories)) {
            return ProfileResult.refused(form, ProfileRefusal.CALORIES);
        }
        if (nameTaken(login, details.displayName())) {
            return ProfileResult.refused(form, ProfileRefusal.NAME_TAKEN);
        }
        return ProfileResult.saved(toForm(userProfileRepository.save(new UserProfile(login, details.age(),
                details.heightCm(), details.weightKg(), details.sex(), details.goal(), details.activityLevel(),
                calories, preferred, excluded, details.email(), details.displayName()))));
    }

    private ProfileResult update(final String login,
                                 final ProfileDetails details,
                                 final ProfileFormDto form,
                                 final UserProfile row,
                                 final String preferred,
                                 final String excluded) {
        final boolean storesFormula = row.getConfirmedCalories() == null;
        final int calories = storesFormula ? formulaCalories(details) : row.getConfirmedCalories();
        if (storesFormula && !personBoundsValidator.acceptsCalories(calories)) {
            return ProfileResult.refused(form, ProfileRefusal.CALORIES);
        }
        if (nameTaken(login, details.displayName())) {
            return ProfileResult.refused(form, ProfileRefusal.NAME_TAKEN);
        }
        if (replace(login, details, row, preferred, excluded, calories) == NO_ROW_UPDATED) {
            return missingRow(form);
        }
        row.replaceValues(details.age(), details.heightCm(), details.weightKg(), details.sex(), details.goal(),
                details.activityLevel(), calories, preferred, excluded, details.email(), details.displayName());
        return ProfileResult.saved(toForm(row));
    }

    private int replace(final String login,
                        final ProfileDetails details,
                        final UserProfile row,
                        final String preferred,
                        final String excluded,
                        final int calories) {
        if (row.getConfirmedCalories() == null) {
            return userProfileRepository.replaceBodyProductsAndCalories(login, details.age(), details.heightCm(),
                    details.weightKg(), details.sex(), details.goal(), details.activityLevel(), calories, preferred,
                    excluded, details.email(), details.displayName());
        }
        return userProfileRepository.replaceBodyAndProducts(login, details.age(), details.heightCm(),
                details.weightKg(), details.sex(), details.goal(), details.activityLevel(), preferred, excluded,
                details.email(), details.displayName());
    }

    private boolean nameTaken(final String login, final String displayName) {
        return householdMemberRepository.findByLoginOrderByMemberIdAsc(login).stream()
                .map(member -> displayNameValidator.matchingKey(member.getDisplayName()))
                .anyMatch(displayNameValidator.matchingKey(displayName)::equals);
    }

    private ProfileResult missingRow(final ProfileFormDto form) {
        LOGGER.warn(NO_ROW_LOG);
        return ProfileResult.unavailable(form);
    }

    private int formulaCalories(final ProfileDetails details) {
        return calorieService.dailyCalories(
                details.age(), details.heightCm(), details.weightKg(), details.sex(), details.activityLevel(),
                details.goal());
    }

    private ProfileFormDto toForm(final UserProfile row) {
        return new ProfileFormDto(
                row.getAge(),
                row.getHeightCm(),
                row.getWeightKg().setScale(WEIGHT_SCALE, RoundingMode.HALF_UP),
                row.getSex(),
                row.getGoal(),
                row.getActivityLevel(),
                productListFormat.split(row.getPreferredProducts()),
                productListFormat.split(row.getExcludedProducts()),
                row.getEmail(),
                row.getDisplayName());
    }

    private void logFailure(final Exception exception) {
        LOGGER.warn(FAILED_LOG, exception.getClass().getName(), causeName(exception));
    }

    private String causeName(final Exception exception) {
        return NestedExceptionUtils.getMostSpecificCause(exception).getClass().getName();
    }
}
