package com.kenez92.plateplan.profile.model;

import com.kenez92.plateplan.profile.controller.dto.ProfileFormDto;

/**
 * What the profile form needs after a load or a write: the body and product fields and whether the
 * database failed or the values were refused. Field errors stay on the controller's
 * {@code BindingResult}. After a load or a successful write the form holds the stored values; after
 * a database failure or a refusal it holds the values the user typed. Daily calories are
 * {@link ConfirmedCaloriesResult}. Expected outcomes are values, not exceptions.
 */
public record ProfileResult(ProfileFormDto profile, boolean unavailable, ProfileRefusal refusal) {

    public static ProfileResult loaded(final ProfileFormDto profile) {
        return new ProfileResult(profile, false, null);
    }

    public static ProfileResult saved(final ProfileFormDto profile) {
        return new ProfileResult(profile, false, null);
    }

    public static ProfileResult unavailable(final ProfileFormDto profile) {
        return new ProfileResult(profile, true, null);
    }

    /**
     * The save wrote nothing. {@code refusal} says why.
     */
    public static ProfileResult refused(final ProfileFormDto profile, final ProfileRefusal refusal) {
        return new ProfileResult(profile, false, refusal);
    }

    /**
     * True when the database answered and a save stored the profile.
     */
    public boolean isSuccessful() {
        return !unavailable && refusal == null;
    }
}
