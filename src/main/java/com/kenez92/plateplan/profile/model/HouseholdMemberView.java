package com.kenez92.plateplan.profile.model;

import com.kenez92.plateplan.profile.controller.dto.HouseholdMemberForm;

/**
 * One added person as the profile screen shows them: the id that later requests use, the body form,
 * and the stored calorie target. The values are personal data, so {@link #toString()} shows none of
 * them.
 */
public record HouseholdMemberView(long memberId, HouseholdMemberForm form, Integer confirmedCalories) {

    private static final String REDACTED = "HouseholdMemberView[redacted]";

    @Override
    public String toString() {
        return REDACTED;
    }
}
