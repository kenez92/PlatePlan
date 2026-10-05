package com.kenez92.plateplan.profile.model;

/**
 * The stored daily calorie target after a load or a write. No profile row on a write is
 * {@link #noProfile()}. Expected outcomes are values, not exceptions.
 */
public record ConfirmedCaloriesResult(Integer dailyCalories, boolean unavailable, boolean missing) {

    public static ConfirmedCaloriesResult loaded(final Integer dailyCalories) {
        return new ConfirmedCaloriesResult(dailyCalories, false, false);
    }

    public static ConfirmedCaloriesResult saved(final int dailyCalories) {
        return new ConfirmedCaloriesResult(dailyCalories, false, false);
    }

    public static ConfirmedCaloriesResult failed() {
        return new ConfirmedCaloriesResult(null, true, false);
    }

    public static ConfirmedCaloriesResult noProfile() {
        return new ConfirmedCaloriesResult(null, false, true);
    }

    /**
     * True when the database answered, which for a write means the target was stored.
     */
    public boolean isSuccessful() {
        return !unavailable;
    }

    public boolean isMissing() {
        return missing;
    }
}
