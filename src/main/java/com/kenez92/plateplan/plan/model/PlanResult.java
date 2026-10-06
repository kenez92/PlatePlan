package com.kenez92.plateplan.plan.model;

/**
 * The diet after a generate attempt. No profile row is {@link #noProfile()}. No stored calorie
 * target is {@link #noCalories()}. The model, the key, a timeout, or the database failing is
 * {@link #unavailable()}. Expected outcomes are values, not exceptions.
 */
public record PlanResult(DietPlan dietPlan,
                         Integer dailyCalories,
                         boolean failed,
                         boolean missingProfile,
                         boolean missingCalories) {

    public static PlanResult success(final DietPlan dietPlan, final int dailyCalories) {
        return new PlanResult(dietPlan, dailyCalories, false, false, false);
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
     * True when the model returned a diet plan.
     */
    public boolean isSuccessful() {
        return dietPlan != null;
    }
}
