package com.kenez92.plateplan.profile.controller;

import java.math.RoundingMode;
import java.security.Principal;
import java.util.List;

import com.kenez92.plateplan.profile.controller.dto.DailyCaloriesForm;
import com.kenez92.plateplan.profile.controller.dto.HouseholdMemberForm;
import com.kenez92.plateplan.profile.controller.dto.ProfileFormDto;
import com.kenez92.plateplan.profile.db.HouseholdMember;
import com.kenez92.plateplan.profile.format.ProductListFormat;
import com.kenez92.plateplan.profile.model.ConfirmedCaloriesResult;
import com.kenez92.plateplan.profile.model.HouseholdMemberError;
import com.kenez92.plateplan.profile.model.HouseholdMemberResult;
import com.kenez92.plateplan.profile.model.HouseholdMemberView;
import com.kenez92.plateplan.profile.model.ProfileRefusal;
import com.kenez92.plateplan.profile.model.ProfileResult;
import com.kenez92.plateplan.profile.service.ConfirmedCaloriesService;
import com.kenez92.plateplan.profile.service.HouseholdMemberService;
import com.kenez92.plateplan.profile.service.ProfileService;
import com.kenez92.plateplan.profile.validator.EmailValidator;
import com.kenez92.plateplan.profile.validator.ProductListsValidator;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private static final String PROFILE_PATH = "/profile";
    private static final String REDIRECT_PREFIX = "redirect:";
    private static final String PROFILE_VIEW = "profile";
    private static final String PROFILE_REDIRECT = REDIRECT_PREFIX + PROFILE_PATH;
    private static final String CALORIES_PATH = "/calories";
    private static final String RECALCULATE_PATH = "/recalculate";
    private static final String MEMBERS_PATH = "/members";
    private static final String MEMBER_ID = "memberId";
    private static final String MEMBER_PATH = "/members/{memberId}";
    private static final String MEMBER_CALORIES_PATH = "/members/{memberId}/calories";
    private static final String MEMBER_RECALCULATE_PATH = "/members/{memberId}/recalculate";
    private static final String MEMBER_DELETE_PATH = "/members/{memberId}/delete";
    private static final String FORM_ATTRIBUTE = "profileForm";
    private static final String CALORIE_FORM_ATTRIBUTE = "calorieForm";
    private static final String MEMBERS_ATTRIBUTE = "members";
    private static final String CREATE_FORM_ATTRIBUTE = "createForm";
    private static final String EDIT_FORM_ATTRIBUTE = "editForm";
    private static final String MEMBER_CALORIE_FORM_ATTRIBUTE = "memberCalorieForm";
    private static final String MEMBER_ERROR_KEY_ATTRIBUTE = "memberErrorKey";
    private static final String MEMBER_ERROR_ID_ATTRIBUTE = "memberErrorId";
    private static final String MEMBER_CALORIE_ERROR_ID_ATTRIBUTE = "memberCalorieErrorId";
    private static final String MEMBER_EDITING_ATTRIBUTE = "memberEditing";
    private static final String SAVED_ATTRIBUTE = "saved";
    private static final String CALORIE_SAVED_ATTRIBUTE = "calorieSaved";
    private static final String MEMBER_SAVED_ATTRIBUTE = "memberSaved";
    private static final String MEMBER_DELETED_ATTRIBUTE = "memberDeleted";
    private static final String LOAD_FAILED_ATTRIBUTE = "loadFailed";
    private static final String SAVE_FAILED_ATTRIBUTE = "saveFailed";
    private static final String CALORIE_SAVE_FAILED_ATTRIBUTE = "calorieSaveFailed";
    private static final String PROFILE_REQUIRED_ATTRIBUTE = "profileRequiredForCalories";
    private static final String OWNER_NAME_TAKEN_ATTRIBUTE = "ownerNameTaken";
    private static final String OWNER_NAME_INVALID_ATTRIBUTE = "ownerNameInvalid";
    private static final String OWNER_CALORIES_REFUSED_ATTRIBUTE = "ownerCaloriesRefused";
    private static final String NAME_TAKEN_KEY = "profile.name.taken";
    private static final String NAME_INVALID_KEY = "profile.name.invalid";
    private static final String CALORIES_REFUSED_KEY = "profile.calories.refused";
    private static final String MEMBER_LIMIT_KEY = "profile.member.limit";
    private static final String MEMBER_MISSING_KEY = "profile.member.missing";
    private static final String MEMBER_NO_OWNER_KEY = "profile.member.noOwner";
    private static final String MEMBER_BODY_KEY = "profile.member.body";
    private static final String MEMBER_PRODUCTS_KEY = "profile.member.products";
    private static final long NO_MEMBER = 0L;
    private static final int WEIGHT_SCALE = 1;

    private final ProfileService profileService;
    private final ConfirmedCaloriesService confirmedCaloriesService;
    private final HouseholdMemberService householdMemberService;
    private final ProductListFormat productListFormat;
    private final ProductListsValidator productListsValidator;
    private final EmailValidator emailValidator;

    public ProfileController(final ProfileService profileService,
                             final ConfirmedCaloriesService confirmedCaloriesService,
                             final HouseholdMemberService householdMemberService,
                             final ProductListFormat productListFormat,
                             final ProductListsValidator productListsValidator,
                             final EmailValidator emailValidator) {
        this.profileService = profileService;
        this.confirmedCaloriesService = confirmedCaloriesService;
        this.householdMemberService = householdMemberService;
        this.productListFormat = productListFormat;
        this.productListsValidator = productListsValidator;
        this.emailValidator = emailValidator;
    }

    @GetMapping
    public String show(final Principal principal, final Model model) {
        final String login = principal.getName();
        final ProfileResult result = profileService.load(login);
        if (!result.isSuccessful()) {
            return loadFailed(model);
        }
        final ConfirmedCaloriesResult calories = confirmedCaloriesService.load(login);
        if (!calories.isSuccessful()) {
            return loadFailed(model);
        }
        model.addAttribute(FORM_ATTRIBUTE, result.profile());
        model.addAttribute(CALORIE_FORM_ATTRIBUTE, new DailyCaloriesForm(calories.dailyCalories()));
        return withMembers(principal, model);
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
        emailValidator.validate(form, bindingResult);
        if (bindingResult.hasErrors()) {
            return withStoredCalories(principal, model);
        }
        final ProfileResult result = profileService.save(principal.getName(), form);
        if (result.refusal() != null) {
            return ownerRefused(result.refusal(), principal, model);
        }
        if (!result.isSuccessful()) {
            model.addAttribute(SAVE_FAILED_ATTRIBUTE, Boolean.TRUE);
            return withStoredCalories(principal, model);
        }
        redirectAttributes.addFlashAttribute(SAVED_ATTRIBUTE, Boolean.TRUE);
        return PROFILE_REDIRECT;
    }

    @PostMapping(MEMBERS_PATH)
    public String createMember(@Valid @ModelAttribute(CREATE_FORM_ATTRIBUTE) final HouseholdMemberForm form,
                               final BindingResult bindingResult,
                               final Principal principal,
                               final Model model,
                               final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return show(principal, model);
        }
        return memberWrite(householdMemberService.create(principal.getName(), form), null, principal, model,
                redirectAttributes);
    }

    @PostMapping(MEMBER_PATH)
    public String saveMember(@PathVariable(MEMBER_ID) final long memberId,
                             @Valid @ModelAttribute(EDIT_FORM_ATTRIBUTE) final HouseholdMemberForm form,
                             final BindingResult bindingResult,
                             final Principal principal,
                             final Model model,
                             final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(MEMBER_ERROR_ID_ATTRIBUTE, memberId);
            model.addAttribute(MEMBER_EDITING_ATTRIBUTE, Boolean.TRUE);
            return show(principal, model);
        }
        return memberWrite(householdMemberService.replaceBody(principal.getName(), memberId, form), memberId,
                principal, model, redirectAttributes);
    }

    @PostMapping(MEMBER_CALORIES_PATH)
    public String updateMemberCalories(@PathVariable(MEMBER_ID) final long memberId,
                                       @Valid @ModelAttribute(MEMBER_CALORIE_FORM_ATTRIBUTE) final DailyCaloriesForm form,
                                       final BindingResult bindingResult,
                                       final Principal principal,
                                       final Model model,
                                       final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(MEMBER_CALORIE_ERROR_ID_ATTRIBUTE, memberId);
            return show(principal, model);
        }
        return memberCalorieWrite(householdMemberService.updateCalories(principal.getName(), memberId,
                form.dailyCalories()), memberId, principal, model, redirectAttributes);
    }

    @PostMapping(MEMBER_RECALCULATE_PATH)
    public String recalculateMember(@PathVariable(MEMBER_ID) final long memberId,
                                    final Principal principal,
                                    final Model model,
                                    final RedirectAttributes redirectAttributes) {
        return memberCalorieWrite(householdMemberService.recalculate(principal.getName(), memberId), memberId,
                principal, model, redirectAttributes);
    }

    @PostMapping(MEMBER_DELETE_PATH)
    public String deleteMember(@PathVariable(MEMBER_ID) final long memberId,
                               final Principal principal,
                               final Model model,
                               final RedirectAttributes redirectAttributes) {
        final HouseholdMemberResult result = householdMemberService.delete(principal.getName(), memberId);
        if (result.error() == HouseholdMemberError.UNAVAILABLE) {
            model.addAttribute(SAVE_FAILED_ATTRIBUTE, Boolean.TRUE);
            return show(principal, model);
        }
        if (result.status() != HouseholdMemberResult.Status.DELETED) {
            model.addAttribute(MEMBER_ERROR_KEY_ATTRIBUTE, memberErrorKey(result.error()));
            return show(principal, model);
        }
        redirectAttributes.addFlashAttribute(MEMBER_DELETED_ATTRIBUTE, Boolean.TRUE);
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

    private String ownerRefused(final ProfileRefusal refusal, final Principal principal, final Model model) {
        model.addAttribute(ownerRefusalAttribute(refusal), Boolean.TRUE);
        return withStoredCalories(principal, model);
    }

    private String ownerRefusalAttribute(final ProfileRefusal refusal) {
        return switch (refusal) {
            case NAME_TAKEN -> OWNER_NAME_TAKEN_ATTRIBUTE;
            case NAME -> OWNER_NAME_INVALID_ATTRIBUTE;
            case CALORIES -> OWNER_CALORIES_REFUSED_ATTRIBUTE;
            case BODY -> SAVE_FAILED_ATTRIBUTE;
        };
    }

    private String memberWrite(final HouseholdMemberResult result,
                               final Long memberId,
                               final Principal principal,
                               final Model model,
                               final RedirectAttributes redirectAttributes) {
        if (result.error() == HouseholdMemberError.UNAVAILABLE) {
            model.addAttribute(SAVE_FAILED_ATTRIBUTE, Boolean.TRUE);
            return show(principal, model);
        }
        if (result.status() == HouseholdMemberResult.Status.SAVED) {
            redirectAttributes.addFlashAttribute(MEMBER_SAVED_ATTRIBUTE, Boolean.TRUE);
            return PROFILE_REDIRECT;
        }
        model.addAttribute(MEMBER_ERROR_KEY_ATTRIBUTE, memberErrorKey(result.error()));
        if (memberId != null && result.error() != HouseholdMemberError.NOT_FOUND) {
            model.addAttribute(MEMBER_ERROR_ID_ATTRIBUTE, memberId);
            model.addAttribute(MEMBER_EDITING_ATTRIBUTE, Boolean.TRUE);
        }
        return show(principal, model);
    }

    private String memberCalorieWrite(final HouseholdMemberResult result,
                                      final long memberId,
                                      final Principal principal,
                                      final Model model,
                                      final RedirectAttributes redirectAttributes) {
        if (result.error() == HouseholdMemberError.UNAVAILABLE) {
            model.addAttribute(CALORIE_SAVE_FAILED_ATTRIBUTE, Boolean.TRUE);
            return show(principal, model);
        }
        if (result.status() == HouseholdMemberResult.Status.SAVED) {
            redirectAttributes.addFlashAttribute(CALORIE_SAVED_ATTRIBUTE, Boolean.TRUE);
            return PROFILE_REDIRECT;
        }
        model.addAttribute(MEMBER_ERROR_KEY_ATTRIBUTE, memberErrorKey(result.error()));
        if (result.error() != HouseholdMemberError.NOT_FOUND) {
            model.addAttribute(MEMBER_ERROR_ID_ATTRIBUTE, memberId);
        }
        return show(principal, model);
    }

    private String memberErrorKey(final HouseholdMemberError error) {
        return switch (error) {
            case NAME_TAKEN -> NAME_TAKEN_KEY;
            case CALORIES -> CALORIES_REFUSED_KEY;
            case LIMIT -> MEMBER_LIMIT_KEY;
            case NOT_FOUND -> MEMBER_MISSING_KEY;
            case NO_OWNER -> MEMBER_NO_OWNER_KEY;
            case NAME -> NAME_INVALID_KEY;
            case BODY -> MEMBER_BODY_KEY;
            case PRODUCTS -> MEMBER_PRODUCTS_KEY;
            case UNAVAILABLE -> null;
        };
    }

    private String withStoredProfile(final Principal principal, final Model model) {
        final ProfileResult result = profileService.load(principal.getName());
        if (!result.isSuccessful()) {
            return loadFailed(model);
        }
        model.addAttribute(FORM_ATTRIBUTE, result.profile());
        return withMembers(principal, model);
    }

    private String withStoredCalories(final Principal principal, final Model model) {
        final ConfirmedCaloriesResult result = confirmedCaloriesService.load(principal.getName());
        if (!result.isSuccessful()) {
            return loadFailed(model);
        }
        model.addAttribute(CALORIE_FORM_ATTRIBUTE, new DailyCaloriesForm(result.dailyCalories()));
        return withMembers(principal, model);
    }

    private String withMembers(final Principal principal, final Model model) {
        final HouseholdMemberResult listed = householdMemberService.list(principal.getName());
        if (listed.status() != HouseholdMemberResult.Status.LISTED) {
            return loadFailed(model);
        }
        model.addAttribute(MEMBERS_ATTRIBUTE, memberViews(listed.members()));
        if (!model.containsAttribute(CREATE_FORM_ATTRIBUTE)) {
            model.addAttribute(CREATE_FORM_ATTRIBUTE, HouseholdMemberForm.empty());
        }
        if (!model.containsAttribute(MEMBER_EDITING_ATTRIBUTE)) {
            model.addAttribute(MEMBER_EDITING_ATTRIBUTE, Boolean.FALSE);
        }
        if (!model.containsAttribute(MEMBER_ERROR_ID_ATTRIBUTE)) {
            model.addAttribute(MEMBER_ERROR_ID_ATTRIBUTE, NO_MEMBER);
        }
        if (!model.containsAttribute(MEMBER_CALORIE_ERROR_ID_ATTRIBUTE)) {
            model.addAttribute(MEMBER_CALORIE_ERROR_ID_ATTRIBUTE, NO_MEMBER);
        }
        return PROFILE_VIEW;
    }

    private List<HouseholdMemberView> memberViews(final List<HouseholdMember> members) {
        return members.stream().map(this::memberView).toList();
    }

    private HouseholdMemberView memberView(final HouseholdMember member) {
        return new HouseholdMemberView(member.getMemberId(), memberForm(member), member.getConfirmedCalories());
    }

    private HouseholdMemberForm memberForm(final HouseholdMember member) {
        return new HouseholdMemberForm(
                member.getDisplayName(),
                member.getAge(),
                member.getHeightCm(),
                member.getWeightKg().setScale(WEIGHT_SCALE, RoundingMode.HALF_UP),
                member.getSex(),
                member.getGoal(),
                member.getActivityLevel(),
                productListFormat.split(member.getPreferredProducts()),
                productListFormat.split(member.getExcludedProducts()));
    }

    private String loadFailed(final Model model) {
        model.addAttribute(LOAD_FAILED_ATTRIBUTE, Boolean.TRUE);
        return PROFILE_VIEW;
    }
}
