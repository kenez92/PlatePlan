package com.kenez92.plateplan.profile.model;

import java.math.BigDecimal;

/**
 * A valid profile body: every field was present and inside its range. The weight has one decimal
 * place. The daily calorie target is not part of this record; a profile save fills or keeps
 * {@code confirmed_calories} on its own.
 */
public record ProfileDetails(int age,
                             int heightCm,
                             BigDecimal weightKg,
                             Sex sex,
                             Goal goal,
                             ActivityLevel activityLevel,
                             ProductLists products) {
}
