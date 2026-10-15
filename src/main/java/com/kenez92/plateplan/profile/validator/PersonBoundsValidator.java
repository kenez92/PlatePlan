package com.kenez92.plateplan.profile.validator;

import java.math.BigDecimal;

import com.kenez92.plateplan.profile.model.ActivityLevel;
import com.kenez92.plateplan.profile.model.Goal;
import com.kenez92.plateplan.profile.model.Sex;
import org.springframework.stereotype.Component;

/**
 * The body bounds and the calorie target shared by the owner and an added person. The weight has
 * at most one decimal place. The calorie target is the same range for a formula result and for a
 * number the client typed.
 */
@Component
public class PersonBoundsValidator {

    private static final int MIN_AGE = 2;
    private static final int MAX_AGE = 110;
    private static final int MIN_HEIGHT_CM = 70;
    private static final int MAX_HEIGHT_CM = 250;
    private static final int MIN_WEIGHT_SCALE = 0;
    private static final int MAX_WEIGHT_SCALE = 1;
    private static final int MIN_CALORIES = 400;
    private static final int MAX_CALORIES = 6000;
    private static final BigDecimal MIN_WEIGHT_KG = new BigDecimal("8.0");
    private static final BigDecimal MAX_WEIGHT_KG = new BigDecimal("400.0");

    public boolean acceptsBody(final Integer age,
                               final Integer heightCm,
                               final BigDecimal weightKg,
                               final Sex sex,
                               final Goal goal,
                               final ActivityLevel activityLevel) {
        return age != null
                && heightCm != null
                && weightKg != null
                && sex != null
                && goal != null
                && activityLevel != null
                && age >= MIN_AGE
                && age <= MAX_AGE
                && heightCm >= MIN_HEIGHT_CM
                && heightCm <= MAX_HEIGHT_CM
                && weightKg.scale() >= MIN_WEIGHT_SCALE
                && weightKg.scale() <= MAX_WEIGHT_SCALE
                && weightKg.compareTo(MIN_WEIGHT_KG) >= 0
                && weightKg.compareTo(MAX_WEIGHT_KG) <= 0;
    }

    public boolean acceptsCalories(final int calories) {
        return calories >= MIN_CALORIES && calories <= MAX_CALORIES;
    }
}
