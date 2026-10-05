package com.kenez92.plateplan.profile.controller;

import java.security.Principal;

import com.kenez92.plateplan.profile.controller.dto.ProfileFormDto;
import com.kenez92.plateplan.profile.model.ProfileResult;
import com.kenez92.plateplan.profile.service.ProfileService;
import com.kenez92.plateplan.profile.validator.ProductListsValidator;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private static final String PROFILE_VIEW = "profile";
    private static final String PROFILE_REDIRECT = "redirect:/profile";
    private static final String FORM_ATTRIBUTE = "profileForm";
    private static final String SAVED_ATTRIBUTE = "saved";
    private static final String LOAD_FAILED_ATTRIBUTE = "loadFailed";
    private static final String SAVE_FAILED_ATTRIBUTE = "saveFailed";

    private final ProfileService profileService;
    private final ProductListsValidator productListsValidator;

    public ProfileController(final ProfileService profileService,
                             final ProductListsValidator productListsValidator) {
        this.profileService = profileService;
        this.productListsValidator = productListsValidator;
    }

    @GetMapping
    public String show(final Principal principal, final Model model) {
        final ProfileResult result = profileService.load(principal.getName());
        if (!result.isSuccessful()) {
            model.addAttribute(LOAD_FAILED_ATTRIBUTE, Boolean.TRUE);
            return PROFILE_VIEW;
        }
        model.addAttribute(FORM_ATTRIBUTE, result.profile());
        return PROFILE_VIEW;
    }

    @PostMapping
    public String save(@Valid @ModelAttribute(FORM_ATTRIBUTE) final ProfileFormDto form,
                       final BindingResult bindingResult,
                       final Principal principal,
                       final Model model,
                       final RedirectAttributes redirectAttributes) {
        productListsValidator.validate(form, bindingResult);
        if (bindingResult.hasErrors()) {
            return PROFILE_VIEW;
        }
        final ProfileResult result = profileService.save(principal.getName(), form);
        if (!result.isSuccessful()) {
            model.addAttribute(SAVE_FAILED_ATTRIBUTE, Boolean.TRUE);
            return PROFILE_VIEW;
        }
        redirectAttributes.addFlashAttribute(SAVED_ATTRIBUTE, Boolean.TRUE);
        return PROFILE_REDIRECT;
    }
}
