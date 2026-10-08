package com.kenez92.plateplan.account.controller;

import java.security.Principal;

import com.kenez92.plateplan.account.controller.dto.RegistrationForm;
import com.kenez92.plateplan.account.model.RegistrationResult;
import com.kenez92.plateplan.account.service.AccountSignInService;
import com.kenez92.plateplan.account.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/register")
public class RegisterController {

    private static final String HOME_PATH = "/";
    private static final String REDIRECT_PREFIX = "redirect:";
    private static final String REGISTER_VIEW = "register";
    private static final String HOME_REDIRECT = REDIRECT_PREFIX + HOME_PATH;
    private static final String FORM_ATTRIBUTE = "registrationForm";
    private static final String ERROR_ATTRIBUTE = "error";

    private final RegistrationService registrationService;
    private final AccountSignInService accountSignInService;

    public RegisterController(final RegistrationService registrationService,
                              final AccountSignInService accountSignInService) {
        this.registrationService = registrationService;
        this.accountSignInService = accountSignInService;
    }

    @GetMapping
    public String register(final Principal principal) {
        if (principal != null) {
            return HOME_REDIRECT;
        }
        return REGISTER_VIEW;
    }

    @PostMapping
    public String register(@ModelAttribute(FORM_ATTRIBUTE) final RegistrationForm form,
                           final Principal principal,
                           final HttpServletRequest request,
                           final HttpServletResponse response,
                           final Model model) {
        if (principal != null) {
            return HOME_REDIRECT;
        }
        final RegistrationResult result = registrationService.register(form.username(), form.password());
        if (!result.isCreated()) {
            model.addAttribute(ERROR_ATTRIBUTE, result.error().name());
            return REGISTER_VIEW;
        }
        accountSignInService.signIn(result.account(), request, response);
        return HOME_REDIRECT;
    }
}
