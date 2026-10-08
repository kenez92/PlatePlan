package com.kenez92.plateplan.profile.service;

import java.util.Optional;

import com.kenez92.plateplan.profile.db.UserProfile;
import com.kenez92.plateplan.profile.db.UserProfileRepository;
import com.kenez92.plateplan.profile.model.ConfirmedCaloriesResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/**
 * Client edits and recalculation of {@code confirmed_calories}. The write is that column only. No
 * profile row is {@link ConfirmedCaloriesResult#noProfile()}. The formula lives in
 * {@link CalorieService}. Body and product fields stay on {@link ProfileService}. Expected
 * outcomes are the stored calories, not a profile form. No message, login, or value is logged; only
 * the class names of the exception and its cause. The service is not transactional on purpose: the
 * exceptions must be caught here, and a failure at commit would escape a transactional proxy.
 */
@Service
public class ConfirmedCaloriesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmedCaloriesService.class);

    private static final String FAILED_LOG = "Profile storage failed on the database: {} (cause: {})";
    private static final int NO_ROW_UPDATED = 0;

    private final UserProfileRepository userProfileRepository;
    private final CalorieService calorieService;

    public ConfirmedCaloriesService(final UserProfileRepository userProfileRepository,
                                    final CalorieService calorieService) {
        this.userProfileRepository = userProfileRepository;
        this.calorieService = calorieService;
    }

    public ConfirmedCaloriesResult load(final String login) {
        try {
            return userProfileRepository.findById(login)
                    .map(row -> ConfirmedCaloriesResult.loaded(row.getConfirmedCalories()))
                    .orElseGet(() -> ConfirmedCaloriesResult.loaded(null));
        } catch (final DataAccessException exception) {
            logFailure(exception);
            return ConfirmedCaloriesResult.failed();
        }
    }

    /**
     * Replaces only {@code confirmed_calories}. No profile row is {@link ConfirmedCaloriesResult#noProfile()}.
     */
    public ConfirmedCaloriesResult update(final String login, final int dailyCalories) {
        try {
            return stored(login, dailyCalories);
        } catch (final DataAccessException exception) {
            logFailure(exception);
            return ConfirmedCaloriesResult.failed();
        }
    }

    /**
     * Stores the formula for the saved body fields. No profile row is {@link ConfirmedCaloriesResult#noProfile()}.
     */
    public ConfirmedCaloriesResult recalculate(final String login) {
        try {
            final Optional<UserProfile> existing = userProfileRepository.findById(login);
            if (existing.isEmpty()) {
                return ConfirmedCaloriesResult.noProfile();
            }
            return stored(login, formulaCalories(existing.get()));
        } catch (final DataAccessException exception) {
            logFailure(exception);
            return ConfirmedCaloriesResult.failed();
        }
    }

    private ConfirmedCaloriesResult stored(final String login, final int confirmedCalories) {
        if (userProfileRepository.replaceConfirmedCalories(login, confirmedCalories) == NO_ROW_UPDATED) {
            return ConfirmedCaloriesResult.noProfile();
        }
        return ConfirmedCaloriesResult.saved(confirmedCalories);
    }

    private int formulaCalories(final UserProfile row) {
        return calorieService.dailyCalories(
                row.getAge(), row.getHeightCm(), row.getWeightKg(), row.getSex(), row.getActivityLevel(),
                row.getGoal());
    }

    private void logFailure(final DataAccessException exception) {
        LOGGER.warn(FAILED_LOG, exception.getClass().getName(), causeName(exception));
    }

    private String causeName(final DataAccessException exception) {
        return NestedExceptionUtils.getMostSpecificCause(exception).getClass().getName();
    }
}
