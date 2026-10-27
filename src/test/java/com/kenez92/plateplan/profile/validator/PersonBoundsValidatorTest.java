package com.kenez92.plateplan.profile.validator;

import com.kenez92.plateplan.profile.model.Goal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PersonBoundsValidatorTest {

    @Test
    void shouldRefuseLoseWeightWhenTheAgeIs17() {
        assertThat(new PersonBoundsValidator().acceptsGoal(17, Goal.LOSE_WEIGHT)).isFalse();
    }

    @Test
    void shouldAcceptLoseWeightWhenTheAgeIs18() {
        assertThat(new PersonBoundsValidator().acceptsGoal(18, Goal.LOSE_WEIGHT)).isTrue();
    }

    @Test
    void shouldAcceptMaintainWhenTheAgeIs17() {
        assertThat(new PersonBoundsValidator().acceptsGoal(17, Goal.MAINTAIN)).isTrue();
    }
}
