package com.kenez92.plateplan.plan.model;

import java.util.Objects;

/**
 * One person the plan screen can select. {@code token} is {@code owner} or a member id as decimal
 * text. {@code name} is the effective name. The delivery email is not here. The name is personal, so
 * {@link #toString()} shows none of it.
 */
public record PlanOption(String token, String name) {

    private static final String REDACTED = "PlanOption[redacted]";

    public PlanOption {
        token = Objects.requireNonNull(token);
        name = Objects.requireNonNull(name);
    }

    @Override
    public String toString() {
        return REDACTED;
    }
}
