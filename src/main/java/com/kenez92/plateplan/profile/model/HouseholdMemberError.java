package com.kenez92.plateplan.profile.model;

/**
 * Why an added person was not stored, not changed, or not removed. Each value is one outcome.
 * {@link #NOT_FOUND} is also the outcome for an id that belongs to another account, and that
 * outcome carries none of that account's values.
 */
public enum HouseholdMemberError {
    NO_OWNER,
    BODY,
    NAME,
    PRODUCTS,
    NAME_TAKEN,
    LIMIT,
    CALORIES,
    NOT_FOUND,
    UNAVAILABLE
}
