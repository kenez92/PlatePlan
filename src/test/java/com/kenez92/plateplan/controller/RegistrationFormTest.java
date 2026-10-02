package com.kenez92.plateplan.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationFormTest {

    @Test
    void shouldNotShowTheLoginOrThePasswordInToString() {
        final RegistrationForm form = new RegistrationForm("alice", "correct horse");

        final String actual = form.toString();

        assertThat(actual).doesNotContain("alice", "correct horse");
    }
}
