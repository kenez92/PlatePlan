package com.kenez92.plateplan.home.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class HomeController {

    private static final String HOME_VIEW = "home";

    @GetMapping
    public String home() {
        return HOME_VIEW;
    }
}
