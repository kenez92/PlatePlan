package com.kenez92.plateplan.plan.model;

import java.util.Objects;

import com.kenez92.plateplan.profile.model.Goal;

/**
 * One selected person as the model should see them: the effective name, the goal, and the confirmed
 * kilocalories. Age, height, weight, sex, activity, the login, and the delivery email are not here.
 */
public record PlanPerson(String name, Goal goal, int kcal) {

    public PlanPerson {
        name = Objects.requireNonNull(name);
        goal = Objects.requireNonNull(goal);
    }
}
