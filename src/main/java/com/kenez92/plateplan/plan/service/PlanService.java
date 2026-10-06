package com.kenez92.plateplan.plan.service;

import com.kenez92.plateplan.plan.model.PlanResult;
import com.kenez92.plateplan.profile.db.UserProfile;
import com.kenez92.plateplan.profile.db.UserProfileRepository;
import com.kenez92.plateplan.profile.format.ProductListFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/**
 * Loads the stored profile and calorie target, then asks {@link DietGenerator} for a day plan. No
 * row is {@link PlanResult#noProfile()}. A null {@code confirmed_calories} is
 * {@link PlanResult#noCalories()}. A database failure is {@link PlanResult#unavailable()}. Only
 * calories and product lists go to the model. No login, product list, or calorie number is logged.
 */
@Service
public class PlanService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlanService.class);

    private static final String FAILED_LOG = "Plan load failed on the database: {} (cause: {})";

    private final UserProfileRepository userProfileRepository;
    private final ProductListFormat productListFormat;
    private final DietGenerator dietGenerator;

    public PlanService(final UserProfileRepository userProfileRepository,
                       final ProductListFormat productListFormat,
                       final DietGenerator dietGenerator) {
        this.userProfileRepository = userProfileRepository;
        this.productListFormat = productListFormat;
        this.dietGenerator = dietGenerator;
    }

    public PlanResult generate(final String login) {
        try {
            return userProfileRepository.findById(login)
                    .map(this::generateFromRow)
                    .orElseGet(PlanResult::noProfile);
        } catch (final DataAccessException exception) {
            logFailure(exception);
            return PlanResult.unavailable();
        }
    }

    private PlanResult generateFromRow(final UserProfile row) {
        final Integer dailyCalories = row.getConfirmedCalories();
        if (dailyCalories == null) {
            return PlanResult.noCalories();
        }
        return dietGenerator.generate(
                dailyCalories,
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
