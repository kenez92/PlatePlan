package com.kenez92.plateplan.plan.controller;

import java.io.IOException;
import java.security.Principal;

import com.kenez92.plateplan.plan.controller.dto.PlanFilesDto;
import com.kenez92.plateplan.plan.model.PlanResult;
import com.kenez92.plateplan.plan.service.PlanPdfWriter;
import com.kenez92.plateplan.plan.service.PlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/plan")
public class PlanController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlanController.class);

    private static final String PLAN_VIEW = "plan";
    private static final String GENERATE_PATH = "/generate";
    private static final String FAILED_LOG = "Plan PDF write failed: {} (cause: {})";

    private final PlanService planService;
    private final PlanPdfWriter planPdfWriter;

    public PlanController(final PlanService planService, final PlanPdfWriter planPdfWriter) {
        this.planService = planService;
        this.planPdfWriter = planPdfWriter;
    }

    @GetMapping
    public String show() {
        return PLAN_VIEW;
    }

    @PostMapping(path = GENERATE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public PlanFilesDto generate(final Principal principal) {
        return toFiles(planService.generate(principal.getName()));
    }

    private PlanFilesDto toFiles(final PlanResult result) {
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
            return PlanFilesDto.success(
                    planPdfWriter.dietPdf(result.dietPlan(), result.dailyCalories()),
                    planPdfWriter.shoppingListPdf(result.dietPlan()));
        } catch (final IOException exception) {
            LOGGER.warn(FAILED_LOG, exception.getClass().getName(),
                    NestedExceptionUtils.getMostSpecificCause(exception).getClass().getName());
            return PlanFilesDto.unavailable();
        }
    }
}
