package com.kenez92.plateplan.plan.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import com.kenez92.plateplan.config.SecurityConfiguration;
import com.kenez92.plateplan.plan.model.DietPlan;
import com.kenez92.plateplan.plan.model.Meal;
import com.kenez92.plateplan.plan.model.PlanResult;
import com.kenez92.plateplan.plan.service.PlanPdfWriter;
import com.kenez92.plateplan.plan.service.PlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(SpringExtension.class)
@WebMvcTest(PlanController.class)
@Import(SecurityConfiguration.class)
@WithMockUser(username = "alice")
class PlanControllerTest {

    private final MockMvc mockMvc;
    private final PlanService planService;
    private final PlanPdfWriter planPdfWriter;

    @Autowired
    PlanControllerTest(final MockMvc mockMvc,
                       final PlanService planService,
                       final PlanPdfWriter planPdfWriter) {
        this.mockMvc = mockMvc;
        this.planService = planService;
        this.planPdfWriter = planPdfWriter;
    }

    @BeforeEach
    void resetTheCollaborators() {
        reset(planService, planPdfWriter);
    }

    @Test
    void shouldShowThePlanPageToASignedInVisitor() throws Exception {
        final String html = mockMvc.perform(get("/plan"))
                .andExpect(status().isOk())
                .andExpect(view().name("plan"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains("Generuj plan", "Pobierz plan", "Pobierz listę zakupów",
                "dieta-na-jutro.pdf", "lista-zakupow.pdf", "href=\"/plan\"", ">Plan<", "href=\"/profile\"",
                "Najpierw zapisz profil, potem możesz wygenerować plan.",
                "Najpierw zapisz cel kaloryczny, potem możesz wygenerować plan.",
                "Nie udało się wygenerować planu. Spróbuj ponownie za chwilę.");
        assertThat(html).doesNotContain("alice's diet");
    }

    @Test
    void shouldReturnTwoBase64PdfsWithoutErrorWhenGenerationSucceeds() throws Exception {
        final byte[] dietPdf = "%PDF-1.4 diet".getBytes(StandardCharsets.ISO_8859_1);
        final byte[] shoppingListPdf = "%PDF-1.4 list".getBytes(StandardCharsets.ISO_8859_1);
        final DietPlan dietPlan = samplePlan();
        when(planService.generate("alice")).thenReturn(PlanResult.success(dietPlan, 2000));
        when(planPdfWriter.dietPdf(dietPlan, 2000)).thenReturn(dietPdf);
        when(planPdfWriter.shoppingListPdf(dietPlan)).thenReturn(shoppingListPdf);

        mockMvc.perform(post("/plan/generate").with(csrf()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.dietPdf").value(Base64.getEncoder().encodeToString(dietPdf)))
                .andExpect(jsonPath("$.shoppingListPdf").value(Base64.getEncoder().encodeToString(shoppingListPdf)));

        verify(planService).generate("alice");
        verify(planService, never()).generate(eq("bob"));
    }

    @Test
    void shouldReturnProfileRequiredWhenThereIsNoProfile() throws Exception {
        when(planService.generate("alice")).thenReturn(PlanResult.noProfile());

        mockMvc.perform(post("/plan/generate").with(csrf()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value("PROFILE_REQUIRED"))
                .andExpect(jsonPath("$.dietPdf").doesNotExist())
                .andExpect(jsonPath("$.shoppingListPdf").doesNotExist());

        verify(planPdfWriter, never()).dietPdf(any(), anyInt());
    }

    @Test
    void shouldReturnCaloriesRequiredWhenTheTargetIsMissing() throws Exception {
        when(planService.generate("alice")).thenReturn(PlanResult.noCalories());

        mockMvc.perform(post("/plan/generate").with(csrf()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value("CALORIES_REQUIRED"))
                .andExpect(jsonPath("$.dietPdf").doesNotExist());

        verify(planPdfWriter, never()).dietPdf(any(), anyInt());
    }

    @Test
    void shouldReturnUnavailableWhenTheModelFails() throws Exception {
        when(planService.generate("alice")).thenReturn(PlanResult.unavailable());

        mockMvc.perform(post("/plan/generate").with(csrf()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.dietPdf").doesNotExist());

        verify(planPdfWriter, never()).dietPdf(any(), anyInt());
    }

    @Test
    void shouldReturnUnavailableWhenPdfWritingFails() throws Exception {
        when(planService.generate("alice")).thenReturn(PlanResult.success(samplePlan(), 2000));
        when(planPdfWriter.dietPdf(any(), anyInt())).thenThrow(new IOException("The writer failed"));

        mockMvc.perform(post("/plan/generate").with(csrf()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.dietPdf").doesNotExist());
    }

    @Test
    void shouldRejectAGeneratePostWithoutACsrfToken() throws Exception {
        mockMvc.perform(post("/plan/generate").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(planService, never()).generate(any());
    }

    private DietPlan samplePlan() {
        return new DietPlan(
                new Meal("Żółć", List.of("jądro"), 400),
                new Meal("Jogurt", List.of("skyr"), 200),
                new Meal("Schabowy", List.of("schab"), 800),
                new Meal("Zupa", List.of("warzywa"), 600),
                List.of("Żółć", "jądro", "chleb"));
    }

    /**
     * Stands in for generation and PDF writing, which are not part of this web slice.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class PlanCollaboratorsStub {

        @Bean
        PlanService planService() {
            return mock(PlanService.class);
        }

        @Bean
        PlanPdfWriter planPdfWriter() {
            return mock(PlanPdfWriter.class);
        }
    }
}
