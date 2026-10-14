package com.kenez92.plateplan.plan.controller;

import java.security.Principal;

import com.kenez92.plateplan.plan.controller.dto.PlanFilesDto;
import com.kenez92.plateplan.plan.model.PlanResult;
import com.kenez92.plateplan.plan.model.WeekPlan;
import com.kenez92.plateplan.plan.service.PlanMailSender;
import com.kenez92.plateplan.plan.service.PlanPdfWriter;
import com.kenez92.plateplan.plan.service.PlanService;
import com.kenez92.plateplan.profile.model.ProfileResult;
import com.kenez92.plateplan.profile.service.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/plan")
public class PlanController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlanController.class);

    private static final String PLAN_VIEW = "plan";
    private static final String GENERATE_PATH = "/generate";
    private static final String SEND_EMAIL_PARAM = "sendEmail";
    private static final String EMAIL_ON_FILE_ATTRIBUTE = "emailOnFile";
    private static final String FALSE_DEFAULT = "false";
    private static final String FAILED_LOG = "Plan PDF write failed: {} (cause: {})";

    private final PlanService planService;
    private final PlanPdfWriter planPdfWriter;
    private final PlanMailSender planMailSender;
    private final ProfileService profileService;

    public PlanController(final PlanService planService,
                          final PlanPdfWriter planPdfWriter,
                          final PlanMailSender planMailSender,
                          final ProfileService profileService) {
        this.planService = planService;
        this.planPdfWriter = planPdfWriter;
        this.planMailSender = planMailSender;
        this.profileService = profileService;
    }

    @GetMapping
    public String show(final Principal principal, final Model model) {
        model.addAttribute(EMAIL_ON_FILE_ATTRIBUTE, profileService.hasDeliveryEmail(principal.getName()));
        return PLAN_VIEW;
    }

    @PostMapping(path = GENERATE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public PlanFilesDto generate(final Principal principal,
                                 @RequestParam(name = SEND_EMAIL_PARAM, defaultValue = FALSE_DEFAULT)
                                 final boolean sendEmail) {
        return toFiles(planService.generate(principal.getName()), principal.getName(), sendEmail);
    }

    private PlanFilesDto toFiles(final PlanResult result, final String login, final boolean sendEmail) {
        if (result.missingProfile()) {
            return PlanFilesDto.profileRequired();
        }
        if (result.missingCalories()) {
            return PlanFilesDto.caloriesRequired();
        }
        if (!result.isSuccessful()) {
            return PlanFilesDto.unavailable();
        }
        try {
            final WeekPlan weekPlan = result.weekPlan();
            final byte[] dietPdf = planPdfWriter.dietPdf(weekPlan, result.dailyCalories());
            final byte[] shoppingListPdf = planPdfWriter.shoppingListPdf(weekPlan);
            if (!sendEmail) {
                return PlanFilesDto.success(dietPdf, shoppingListPdf);
            }
            return sendPdfs(login, dietPdf, shoppingListPdf);
        } catch (final Exception exception) {
            LOGGER.warn(FAILED_LOG, exception.getClass().getName(),
                    NestedExceptionUtils.getMostSpecificCause(exception).getClass().getName());
            return PlanFilesDto.unavailable();
        }
    }

    private PlanFilesDto sendPdfs(final String login, final byte[] dietPdf, final byte[] shoppingListPdf) {
        final ProfileResult profile = profileService.load(login);
        final String to = profile.isSuccessful() ? profile.profile().email() : null;
        if (to == null || !planMailSender.send(to, dietPdf, shoppingListPdf)) {
            return PlanFilesDto.successWithEmailUnavailable(dietPdf, shoppingListPdf);
        }
        return PlanFilesDto.success(dietPdf, shoppingListPdf);
    }
}
