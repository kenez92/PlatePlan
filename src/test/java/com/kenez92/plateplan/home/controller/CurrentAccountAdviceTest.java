package com.kenez92.plateplan.home.controller;

import java.security.Principal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentAccountAdviceTest {

    @Test
    void shouldExposeTheLoginOfASignedInVisitor() {
        final CurrentAccountAdvice advice = new CurrentAccountAdvice();
        final Principal principal = () -> "alice";

        final String actual = advice.currentLogin(principal);

        assertThat(actual).isEqualTo("alice");
    }

    @Test
    void shouldExposeNothingForAnAnonymousVisitor() {
        final CurrentAccountAdvice advice = new CurrentAccountAdvice();

        final String actual = advice.currentLogin(null);

        assertThat(actual).isNull();
    }
}
