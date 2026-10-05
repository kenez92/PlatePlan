package com.kenez92.plateplan.profile.service;

import java.math.BigDecimal;

import com.kenez92.plateplan.profile.model.ActivityLevel;
import com.kenez92.plateplan.profile.model.Goal;
import com.kenez92.plateplan.profile.model.Sex;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalorieServiceTest {

    @Test
    void shouldReturnLockedMaleModerateCaloriesForEachGoal() {
        final CalorieService service = new CalorieService();
        final BigDecimal weightKg = new BigDecimal("82.5");
        final int maintain = 2767;
        final int loseWeight = 2517;
        final int gain = 3017;

        final int maintainActual = service.dailyCalories(34, 180, weightKg, Sex.MALE, ActivityLevel.MODERATE,
                Goal.MAINTAIN);
        final int loseWeightActual = service.dailyCalories(34, 180, weightKg, Sex.MALE, ActivityLevel.MODERATE,
                Goal.LOSE_WEIGHT);
        final int gainActual = service.dailyCalories(34, 180, weightKg, Sex.MALE, ActivityLevel.MODERATE,
                Goal.GAIN);

        assertThat(maintainActual).isEqualTo(maintain);
        assertThat(loseWeightActual).isEqualTo(loseWeight);
        assertThat(gainActual).isEqualTo(gain);
    }

    @Test
    void shouldScaleTheSameMaleBmrByEachOtherActivityWhenMaintaining() {
        final CalorieService service = new CalorieService();
        final BigDecimal weightKg = new BigDecimal("82.5");
        final int sedentary = 2142;
        final int light = 2454;
        final int high = 3079;

        final int sedentaryActual = service.dailyCalories(34, 180, weightKg, Sex.MALE, ActivityLevel.SEDENTARY,
                Goal.MAINTAIN);
        final int lightActual = service.dailyCalories(34, 180, weightKg, Sex.MALE, ActivityLevel.LIGHT,
                Goal.MAINTAIN);
        final int highActual = service.dailyCalories(34, 180, weightKg, Sex.MALE, ActivityLevel.HIGH,
                Goal.MAINTAIN);

        assertThat(sedentaryActual).isEqualTo(sedentary);
        assertThat(lightActual).isEqualTo(light);
        assertThat(highActual).isEqualTo(high);
    }

    @Test
    void shouldReturnFemaleLightMaintainCalories() {
        final CalorieService service = new CalorieService();
        final BigDecimal weightKg = new BigDecimal("60.0");
        final int expected = 1815;

        final int actual = service.dailyCalories(30, 165, weightKg, Sex.FEMALE, ActivityLevel.LIGHT,
                Goal.MAINTAIN);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldReturnANegativeNumberWhenThereIsNoFloor() {
        final CalorieService service = new CalorieService();
        final BigDecimal weightKg = new BigDecimal("20.0");
        final int expected = -263;

        final int actual = service.dailyCalories(110, 80, weightKg, Sex.FEMALE, ActivityLevel.SEDENTARY,
                Goal.LOSE_WEIGHT);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldUseALightDeficitForAMaleLosingWeight() {
        final CalorieService service = new CalorieService();
        final BigDecimal weightKg = new BigDecimal("77.0");
        final int expected = 1751;

        final int actual = service.dailyCalories(34, 170, weightKg, Sex.MALE, ActivityLevel.SEDENTARY,
                Goal.LOSE_WEIGHT);

        assertThat(actual).isEqualTo(expected);
    }
}
