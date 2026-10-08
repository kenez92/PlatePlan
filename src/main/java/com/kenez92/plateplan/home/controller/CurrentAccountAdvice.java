package com.kenez92.plateplan.home.controller;

import java.security.Principal;
import java.util.Optional;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Every page shares one header, and the header differs for a signed-in visitor. The Thymeleaf
 * Security extras are not a dependency, so the signed-in login reaches the view through the model.
 * It reads the session only and never the database.
 */
@ControllerAdvice
public class CurrentAccountAdvice {

    private static final String CURRENT_LOGIN = "currentLogin";

    @ModelAttribute(CURRENT_LOGIN)
    public String currentLogin(final Principal principal) {
        return Optional.ofNullable(principal).map(Principal::getName).orElse(null);
    }
}
