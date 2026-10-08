package com.kenez92.plateplan.profile.service;

import java.math.RoundingMode;
import java.util.Optional;

import com.kenez92.plateplan.profile.controller.dto.ProfileFormDto;
import com.kenez92.plateplan.profile.db.UserProfile;
import com.kenez92.plateplan.profile.db.UserProfileRepository;
import com.kenez92.plateplan.profile.format.ProductListFormat;
import com.kenez92.plateplan.profile.model.ProductLists;
import com.kenez92.plateplan.profile.model.ProfileDetails;
import com.kenez92.plateplan.profile.model.ProfileResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/**
 * Reads and writes the profile body and product lists. It queries the database only when a request
 * arrives. The controller validates the form first; this service stores what it is given. A profile
 * save never takes a calorie number from the client: the first save stores the formula, and a later
 * save keeps the stored target. Client edits and recalculation of {@code confirmed_calories} are
 * {@link ConfirmedCaloriesService}. Expected outcomes are returned as a {@link ProfileResult}, not
 * thrown. The profile is personal data and a unique-index violation carries the login in its
 * message, so no message, login, or value is logged or passed on; only the class names of the
 * exception and its cause are logged. An existing row is replaced in one statement, so a later
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
    private final ProductListFormat productListFormat;
    private final CalorieService calorieService;

    public ProfileService(final UserProfileRepository userProfileRepository,
                          final ProductListFormat productListFormat,
                          final CalorieService calorieService) {
        this.userProfileRepository = userProfileRepository;
        this.productListFormat = productListFormat;
        this.calorieService = calorieService;
    }

    public ProfileResult load(final String login) {
        try {
            return userProfileRepository.findById(login)
                    .map(this::toForm)
                    .map(ProfileResult::loaded)
                    .orElseGet(() -> ProfileResult.loaded(ProfileFormDto.empty()));
        } catch (final DataAccessException exception) {
            logFailure(exception);
            return ProfileResult.unavailable(ProfileFormDto.empty());
        }
    }

    public ProfileResult save(final String login, final ProfileFormDto form) {
        try {
            return store(login, toDetails(form))
                    .map(this::toForm)
                    .map(ProfileResult::saved)
                    .orElseGet(() -> missingRow(form));
        } catch (final DataAccessException exception) {
            logFailure(exception);
            return ProfileResult.unavailable(form);
        }
    }

    private ProfileDetails toDetails(final ProfileFormDto form) {
        return new ProfileDetails(
                form.age(),
                form.heightCm(),
                form.weightKg(),
                form.sex(),
                form.goal(),
                form.activityLevel(),
                new ProductLists(form.preferredProducts(), form.excludedProducts()));
    }

    private Optional<UserProfile> store(final String login, final ProfileDetails details) {
        final String preferred = productListFormat.join(details.products().preferred());
        final String excluded = productListFormat.join(details.products().excluded());
        final Optional<UserProfile> existing = userProfileRepository.findById(login);
        if (existing.isEmpty()) {
            return Optional.of(userProfileRepository.save(new UserProfile(login, details.age(), details.heightCm(),
                    details.weightKg(), details.sex(), details.goal(), details.activityLevel(),
                    formulaCalories(details), preferred, excluded)));
        }
        return replaceExisting(login, details, existing.get(), preferred, excluded);
    }

    private Optional<UserProfile> replaceExisting(final String login,
                                                  final ProfileDetails details,
                                                  final UserProfile row,
                                                  final String preferred,
                                                  final String excluded) {
        final int calories = confirmedCalories(row, details);
        if (replace(login, details, row, preferred, excluded, calories) == NO_ROW_UPDATED) {
            return Optional.empty();
        }
        row.replaceValues(details.age(), details.heightCm(), details.weightKg(), details.sex(), details.goal(),
                details.activityLevel(), calories, preferred, excluded);
        return Optional.of(row);
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
                    excluded);
        }
        return userProfileRepository.replaceBodyAndProducts(login, details.age(), details.heightCm(),
                details.weightKg(), details.sex(), details.goal(), details.activityLevel(), preferred, excluded);
    }

    private ProfileResult missingRow(final ProfileFormDto form) {
        LOGGER.warn(NO_ROW_LOG);
        return ProfileResult.unavailable(form);
    }

    private Integer confirmedCalories(final UserProfile row, final ProfileDetails details) {
        if (row.getConfirmedCalories() != null) {
            return row.getConfirmedCalories();
        }
        return formulaCalories(details);
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
                productListFormat.split(row.getExcludedProducts()));
    }

    private void logFailure(final DataAccessException exception) {
        LOGGER.warn(FAILED_LOG, exception.getClass().getName(), causeName(exception));
    }

    private String causeName(final DataAccessException exception) {
        return NestedExceptionUtils.getMostSpecificCause(exception).getClass().getName();
    }
}
