package com.kenez92.plateplan.profile.controller.dto;

import java.math.BigDecimal;
import java.util.List;

import com.kenez92.plateplan.profile.model.ActivityLevel;
import com.kenez92.plateplan.profile.model.Goal;
import com.kenez92.plateplan.profile.model.Sex;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileFormDtoTest {

    @Test
    void shouldNotShowAnyValueInToString() {
        final ProfileFormDto form = new ProfileFormDto(34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN,
                ActivityLevel.MODERATE, List.of("mleko 3,2%"), List.of("orzechy"));

        final String actual = form.toString();

        assertThat(actual).doesNotContain("34", "180", "82.5", "MALE", "MAINTAIN", "MODERATE", "mleko", "orzechy");
    }

    @Test
    void shouldKeepTheFirstSpellingWhenAProductRepeatsIgnoringCase() {
        final ProfileFormDto form = new ProfileFormDto(34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN,
                ActivityLevel.MODERATE, List.of("Mleko", "mleko", "JAJKA"), List.of("Orzechy", "orzechy"));

        assertThat(form).usingRecursiveComparison().isEqualTo(new ProfileFormDto(34, 180, new BigDecimal("82.5"),
                Sex.MALE, Goal.MAINTAIN, ActivityLevel.MODERATE, List.of("Mleko", "JAJKA"), List.of("Orzechy")));
    }
}
