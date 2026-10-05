package com.kenez92.plateplan.profile.controller.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DailyCaloriesFormTest {

    @Test
    void shouldNotShowAnyValueInToString() {
        final DailyCaloriesForm form = new DailyCaloriesForm(2000);

        final String actual = form.toString();

        assertThat(actual).doesNotContain("2000");
    }
}
