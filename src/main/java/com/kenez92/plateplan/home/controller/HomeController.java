package com.kenez92.plateplan.home.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class HomeController {

    private static final String HOME_VIEW = "home";
    private static final String PLAN_PATH = "/plan";
    private static final String REDIRECT_PREFIX = "redirect:";
    private static final String PLAN_REDIRECT = REDIRECT_PREFIX + PLAN_PATH;

    @GetMapping
    public String home(final Principal principal) {
        if (principal != null) {
            return PLAN_REDIRECT;
        }
        return HOME_VIEW;
    }
}
