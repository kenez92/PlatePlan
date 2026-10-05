package com.kenez92.plateplan.profile.model;

import com.kenez92.plateplan.profile.controller.dto.ProfileFormDto;

/**
 * What the profile form needs after a load or a write: the body and product fields and whether the
 * database failed. Field errors stay on the controller's {@code BindingResult}. After a load or a
 * successful write the form holds the stored values; after a database failure it holds the values
 * the user typed. Daily calories are {@link ConfirmedCaloriesResult}. Expected outcomes are values,
 * not exceptions.
 */
public record ProfileResult(ProfileFormDto profile, boolean unavailable) {

    public static ProfileResult loaded(final ProfileFormDto profile) {
        return new ProfileResult(profile, false);
    }

    public static ProfileResult saved(final ProfileFormDto profile) {
        return new ProfileResult(profile, false);
    }

    public static ProfileResult unavailable(final ProfileFormDto profile) {
        return new ProfileResult(profile, true);
    }

    /**
     * True when the database answered, which for a save means the profile was stored.
     */
    public boolean isSuccessful() {
        return !unavailable;
    }
}
