package com.kenez92.plateplan.plan.model;

/**
 * The week after a generate attempt. No profile row is {@link #noProfile()}. No stored calorie
 * target is {@link #noCalories()}. The model, the key, a timeout, a rejected week, or the database
 * failing is {@link #unavailable()}. Expected outcomes are values, not exceptions.
 */
public record PlanResult(WeekPlan weekPlan,
                         Integer dailyCalories,
                         boolean failed,
                         boolean missingProfile,
                         boolean missingCalories) {

    public static PlanResult success(final WeekPlan weekPlan, final int dailyCalories) {
        return new PlanResult(weekPlan, dailyCalories, false, false, false);
    }

    public static PlanResult unavailable() {
        return new PlanResult(null, null, true, false, false);
    }

    public static PlanResult noProfile() {
        return new PlanResult(null, null, false, true, false);
    }

    public static PlanResult noCalories() {
        return new PlanResult(null, null, false, false, true);
    }

    /**
     * True when the model returned an accepted week.
     */
    public boolean isSuccessful() {
        return weekPlan != null && !failed;
    }
}
