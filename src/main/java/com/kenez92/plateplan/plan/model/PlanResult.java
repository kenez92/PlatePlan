package com.kenez92.plateplan.plan.model;

/**
 * The week after a generate attempt. No profile row is {@link #noProfile()}. No stored calorie
 * target on a selected person is {@link #noCalories()}. An empty selection is {@link #noSelection()}.
 * The model, the key, a timeout, a rejected week, or the database failing is {@link #unavailable()}.
 * Expected outcomes are values, not exceptions. {@code dailyCalories} is the first selected person's
 * target, which the current diet file still prints as one daily line.
 */
public record PlanResult(WeekPlan weekPlan,
                         Integer dailyCalories,
                         boolean failed,
                         boolean missingProfile,
                         boolean missingCalories,
                         boolean missingSelection) {

    public static PlanResult success(final WeekPlan weekPlan, final int dailyCalories) {
        return new PlanResult(weekPlan, dailyCalories, false, false, false, false);
    }

    public static PlanResult unavailable() {
        return new PlanResult(null, null, true, false, false, false);
    }

    public static PlanResult noProfile() {
        return new PlanResult(null, null, false, true, false, false);
    }

    public static PlanResult noCalories() {
        return new PlanResult(null, null, false, false, true, false);
    }

    public static PlanResult noSelection() {
        return new PlanResult(null, null, false, false, false, true);
    }

    /**
     * True when the model returned an accepted week.
     */
    public boolean isSuccessful() {
        return weekPlan != null && !failed;
    }
}
