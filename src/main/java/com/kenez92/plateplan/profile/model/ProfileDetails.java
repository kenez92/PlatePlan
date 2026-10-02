package com.kenez92.plateplan.profile.model;

import java.math.BigDecimal;

/**
 * A valid profile: every field was present and inside its range. The weight has one decimal place.
 */
public record ProfileDetails(int age,
                             int heightCm,
                             BigDecimal weightKg,
                             Sex sex,
                             Goal goal,
                             ActivityLevel activityLevel,
                             ProductLists products) {
}
