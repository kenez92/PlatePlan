package com.kenez92.plateplan.plan.model;

import java.util.List;

/**
 * The week after a generate attempt. No profile row is {@link #noProfile()}. No stored calorie
 * target on a selected person is {@link #noCalories()}. An empty selection, or a token that matches
 * nobody on the account, is {@link #noSelection()}. The model, the key, a timeout, a rejected week,
 * or the database failing is {@link #unavailable()}. Expected outcomes are values, not exceptions.
 * {@code people} is the selected people in household order, each with the account calorie target,
 * when the week was accepted.
 */
public record PlanResult(WeekPlan weekPlan,
                         List<PlanPerson> people,
                         boolean failed,
                         boolean missingProfile,
                         boolean missingCalories,
                         boolean missingSelection) {

    public PlanResult {
        people = people == null ? null : List.copyOf(people);
    }

    public static PlanResult success(final WeekPlan weekPlan, final List<PlanPerson> people) {
        return new PlanResult(weekPlan, people, false, false, false, false);
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
