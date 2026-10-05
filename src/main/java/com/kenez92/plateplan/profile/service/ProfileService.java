package com.kenez92.plateplan.profile.service;

import java.math.RoundingMode;

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
 * The only place that reads or writes the profile table. It queries the database only when a request
 * arrives. The controller validates the form first; this service stores what it is given. Expected
 * outcomes are returned as a {@link ProfileResult}, not thrown. The profile is personal data and a
 * unique-index violation carries the login in its message, so no message, login, or value is logged
 * or passed on; only the class names of the exception and its cause are logged. The service is not
 * transactional on purpose: the exceptions must be caught here, and a failure at commit would escape
 * a transactional proxy.
 */
@Service
public class ProfileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileService.class);

    private static final String FAILED_LOG = "Profile storage failed on the database: {} (cause: {})";
    private static final int WEIGHT_SCALE = 1;

    private final UserProfileRepository userProfileRepository;
    private final ProductListFormat productListFormat;

    public ProfileService(final UserProfileRepository userProfileRepository,
                          final ProductListFormat productListFormat) {
        this.userProfileRepository = userProfileRepository;
        this.productListFormat = productListFormat;
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
            final UserProfile stored = userProfileRepository.save(toRow(login, toDetails(form)));
            return ProfileResult.saved(toForm(stored));
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

    private UserProfile toRow(final String login, final ProfileDetails details) {
        final String preferred = productListFormat.join(details.products().preferred());
        final String excluded = productListFormat.join(details.products().excluded());
        return userProfileRepository.findById(login)
                .map(row -> {
                    row.replaceValues(details.age(), details.heightCm(), details.weightKg(), details.sex(),
                            details.goal(), details.activityLevel(), preferred, excluded);
                    return row;
                })
                .orElseGet(() -> new UserProfile(login, details.age(), details.heightCm(), details.weightKg(),
                        details.sex(), details.goal(), details.activityLevel(), preferred, excluded));
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
