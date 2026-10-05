package com.kenez92.plateplan.profile.controller;

import java.security.Principal;

import com.kenez92.plateplan.profile.controller.dto.DailyCaloriesForm;
import com.kenez92.plateplan.profile.controller.dto.ProfileFormDto;
import com.kenez92.plateplan.profile.model.ConfirmedCaloriesResult;
import com.kenez92.plateplan.profile.model.ProfileResult;
import com.kenez92.plateplan.profile.service.ConfirmedCaloriesService;
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
    private static final String CALORIES_PATH = "/calories";
    private static final String RECALCULATE_PATH = "/recalculate";
    private static final String FORM_ATTRIBUTE = "profileForm";
    private static final String CALORIE_FORM_ATTRIBUTE = "calorieForm";
    private static final String SAVED_ATTRIBUTE = "saved";
    private static final String CALORIE_SAVED_ATTRIBUTE = "calorieSaved";
    private static final String LOAD_FAILED_ATTRIBUTE = "loadFailed";
    private static final String SAVE_FAILED_ATTRIBUTE = "saveFailed";
    private static final String CALORIE_SAVE_FAILED_ATTRIBUTE = "calorieSaveFailed";
    private static final String PROFILE_REQUIRED_ATTRIBUTE = "profileRequiredForCalories";

    private final ProfileService profileService;
    private final ConfirmedCaloriesService confirmedCaloriesService;
    private final ProductListsValidator productListsValidator;

    public ProfileController(final ProfileService profileService,
                             final ConfirmedCaloriesService confirmedCaloriesService,
                             final ProductListsValidator productListsValidator) {
        this.profileService = profileService;
        this.confirmedCaloriesService = confirmedCaloriesService;
        this.productListsValidator = productListsValidator;
    }

    @GetMapping
    public String show(final Principal principal, final Model model) {
        final ProfileResult result = profileService.load(principal.getName());
        if (!result.isSuccessful()) {
            model.addAttribute(LOAD_FAILED_ATTRIBUTE, Boolean.TRUE);
            return PROFILE_VIEW;
        }
        final ConfirmedCaloriesResult calories = confirmedCaloriesService.load(principal.getName());
        if (!calories.isSuccessful()) {
            model.addAttribute(LOAD_FAILED_ATTRIBUTE, Boolean.TRUE);
            return PROFILE_VIEW;
        }
        model.addAttribute(FORM_ATTRIBUTE, result.profile());
        model.addAttribute(CALORIE_FORM_ATTRIBUTE, new DailyCaloriesForm(calories.dailyCalories()));
        return PROFILE_VIEW;
    }

    @PostMapping(CALORIES_PATH)
    public String updateCalories(@Valid @ModelAttribute(CALORIE_FORM_ATTRIBUTE) final DailyCaloriesForm form,
                                 final BindingResult bindingResult,
                                 final Principal principal,
                                 final Model model,
                                 final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return withStoredProfile(principal, model);
        }
        return calorieWrite(confirmedCaloriesService.update(principal.getName(), form.dailyCalories()), principal,
                model, redirectAttributes);
    }

    @PostMapping(RECALCULATE_PATH)
    public String recalculate(final Principal principal,
                              final Model model,
                              final RedirectAttributes redirectAttributes) {
        return calorieWrite(confirmedCaloriesService.recalculate(principal.getName()), principal, model,
                redirectAttributes);
    }

    @PostMapping
    public String save(@Valid @ModelAttribute(FORM_ATTRIBUTE) final ProfileFormDto form,
                       final BindingResult bindingResult,
                       final Principal principal,
                       final Model model,
                       final RedirectAttributes redirectAttributes) {
        productListsValidator.validate(form, bindingResult);
        if (bindingResult.hasErrors()) {
            return withStoredCalories(principal, model);
        }
        final ProfileResult result = profileService.save(principal.getName(), form);
        if (!result.isSuccessful()) {
            model.addAttribute(SAVE_FAILED_ATTRIBUTE, Boolean.TRUE);
            return withStoredCalories(principal, model);
        }
        redirectAttributes.addFlashAttribute(SAVED_ATTRIBUTE, Boolean.TRUE);
        return PROFILE_REDIRECT;
    }

    private String calorieWrite(final ConfirmedCaloriesResult result,
                                final Principal principal,
                                final Model model,
                                final RedirectAttributes redirectAttributes) {
        if (result.isMissing()) {
            model.addAttribute(PROFILE_REQUIRED_ATTRIBUTE, Boolean.TRUE);
            return show(principal, model);
        }
        if (!result.isSuccessful()) {
            model.addAttribute(CALORIE_SAVE_FAILED_ATTRIBUTE, Boolean.TRUE);
            return show(principal, model);
        }
        redirectAttributes.addFlashAttribute(CALORIE_SAVED_ATTRIBUTE, Boolean.TRUE);
        return PROFILE_REDIRECT;
    }

    private String withStoredProfile(final Principal principal, final Model model) {
        final ProfileResult result = profileService.load(principal.getName());
        if (!result.isSuccessful()) {
            model.addAttribute(LOAD_FAILED_ATTRIBUTE, Boolean.TRUE);
            return PROFILE_VIEW;
        }
        model.addAttribute(FORM_ATTRIBUTE, result.profile());
        return PROFILE_VIEW;
    }

    private String withStoredCalories(final Principal principal, final Model model) {
        final ConfirmedCaloriesResult result = confirmedCaloriesService.load(principal.getName());
        if (result.isSuccessful()) {
            model.addAttribute(CALORIE_FORM_ATTRIBUTE, new DailyCaloriesForm(result.dailyCalories()));
        } else {
            model.addAttribute(CALORIE_FORM_ATTRIBUTE, DailyCaloriesForm.empty());
        }
        return PROFILE_VIEW;
    }
}
