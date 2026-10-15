package com.kenez92.plateplan.profile.model;

/**
 * The stored daily calorie target after a load or a write. No profile row on a write is
 * {@link #noProfile()}. A number outside the calorie range is {@link #refused(Integer)} and leaves
 * the previous number in place. Expected outcomes are values, not exceptions.
 */
public record ConfirmedCaloriesResult(Integer dailyCalories, boolean unavailable, boolean missing, boolean refused) {

    public static ConfirmedCaloriesResult loaded(final Integer dailyCalories) {
        return new ConfirmedCaloriesResult(dailyCalories, false, false, false);
    }

    public static ConfirmedCaloriesResult saved(final int dailyCalories) {
        return new ConfirmedCaloriesResult(dailyCalories, false, false, false);
    }

    public static ConfirmedCaloriesResult failed() {
        return new ConfirmedCaloriesResult(null, true, false, false);
    }

    public static ConfirmedCaloriesResult noProfile() {
        return new ConfirmedCaloriesResult(null, false, true, false);
    }

    /**
     * The write did not happen. {@code dailyCalories} is the number that stayed stored, or null when
     * the call did not read one.
     */
    public static ConfirmedCaloriesResult refused(final Integer dailyCalories) {
        return new ConfirmedCaloriesResult(dailyCalories, false, false, true);
    }

    /**
     * True when the database answered and a write stored the target.
     */
    public boolean isSuccessful() {
        return !unavailable && !refused;
    }

    public boolean isMissing() {
        return missing;
    }
}
