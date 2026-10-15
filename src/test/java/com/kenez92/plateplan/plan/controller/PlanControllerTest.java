package com.kenez92.plateplan.plan.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import com.kenez92.plateplan.config.SecurityConfiguration;
import com.kenez92.plateplan.plan.model.DayMeals;
import com.kenez92.plateplan.plan.model.Ingredient;
import com.kenez92.plateplan.plan.model.Meal;
import com.kenez92.plateplan.plan.model.PlanResult;
import com.kenez92.plateplan.plan.model.Portion;
import com.kenez92.plateplan.plan.model.ShoppingItem;
import com.kenez92.plateplan.plan.model.WeekPlan;
import com.kenez92.plateplan.plan.service.PlanMailSender;
import com.kenez92.plateplan.plan.service.PlanPdfWriter;
import com.kenez92.plateplan.plan.service.PlanService;
import com.kenez92.plateplan.profile.controller.dto.ProfileFormDto;
import com.kenez92.plateplan.profile.model.ActivityLevel;
import com.kenez92.plateplan.profile.model.Goal;
import com.kenez92.plateplan.profile.model.ProfileResult;
import com.kenez92.plateplan.profile.model.Sex;
import com.kenez92.plateplan.profile.service.ProfileService;
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
    private final PlanMailSender planMailSender;
    private final ProfileService profileService;

    @Autowired
    PlanControllerTest(final MockMvc mockMvc,
                       final PlanService planService,
                       final PlanPdfWriter planPdfWriter,
                       final PlanMailSender planMailSender,
                       final ProfileService profileService) {
        this.mockMvc = mockMvc;
        this.planService = planService;
        this.planPdfWriter = planPdfWriter;
        this.planMailSender = planMailSender;
        this.profileService = profileService;
    }

    @BeforeEach
    void resetTheCollaborators() {
        reset(planService, planPdfWriter, planMailSender, profileService);
        when(profileService.hasDeliveryEmail("alice")).thenReturn(false);
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
                "dieta-na-tydzien.pdf", "lista-zakupow.pdf", "Na 7 dni", "siedem dni", "cztery posiłki",
                "href=\"/plan\"", ">Plan<", "href=\"/profile\"",
                "Najpierw zapisz profil, potem możesz wygenerować plan.",
                "Najpierw zapisz cel kaloryczny, potem możesz wygenerować plan.",
                "Nie udało się wygenerować planu. Spróbuj ponownie za chwilę.");
        assertThat(html).doesNotContain("alice's diet", "id=\"send-email\"", "Wyślij plan na e-mail", "jutro");
    }

    @Test
    void shouldShowTheEmailCheckboxWhenTheProfileHasAnAddress() throws Exception {
        when(profileService.hasDeliveryEmail("alice")).thenReturn(true);

        final String html = mockMvc.perform(get("/plan"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains("id=\"send-email\"", "Wyślij plan na e-mail");
        assertThat(html).doesNotContain("checked");
    }

    @Test
    void shouldReturnTwoBase64PdfsWithoutErrorWhenGenerationSucceeds() throws Exception {
        final byte[] dietPdf = "%PDF-1.4 diet".getBytes(StandardCharsets.ISO_8859_1);
        final byte[] shoppingListPdf = "%PDF-1.4 list".getBytes(StandardCharsets.ISO_8859_1);
        final WeekPlan weekPlan = samplePlan();
        when(planService.generate("alice")).thenReturn(PlanResult.success(weekPlan, 2000));
        when(planPdfWriter.dietPdf(weekPlan, 2000)).thenReturn(dietPdf);
        when(planPdfWriter.shoppingListPdf(weekPlan)).thenReturn(shoppingListPdf);

        mockMvc.perform(post("/plan/generate").with(csrf()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.dietPdf").value(Base64.getEncoder().encodeToString(dietPdf)))
                .andExpect(jsonPath("$.shoppingListPdf").value(Base64.getEncoder().encodeToString(shoppingListPdf)))
                .andExpect(jsonPath("$.emailError").doesNotExist());

        verify(planService).generate("alice");
        verify(planService, never()).generate(eq("bob"));
        verify(planMailSender, never()).send(any(), any(), any());
    }

    @Test
    void shouldGenerateThePlanOfTheSignedInLoginOnly() throws Exception {
        when(planService.generate("alice")).thenReturn(PlanResult.success(samplePlan(), 2000));
        when(planPdfWriter.dietPdf(any(), anyInt())).thenReturn("%PDF-1.4 diet".getBytes(StandardCharsets.ISO_8859_1));
        when(planPdfWriter.shoppingListPdf(any())).thenReturn("%PDF-1.4 list".getBytes(StandardCharsets.ISO_8859_1));

        mockMvc.perform(post("/plan/generate").with(csrf()).accept(MediaType.APPLICATION_JSON).param("login", "bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(planService).generate("alice");
        verify(planService, never()).generate(eq("bob"));
    }

    @Test
    void shouldNotSendMailWhenGenerationFailsEvenIfSendEmailIsTrue() throws Exception {
        when(planService.generate("alice")).thenReturn(PlanResult.noProfile());

        mockMvc.perform(post("/plan/generate").with(csrf()).param("sendEmail", "true").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value("PROFILE_REQUIRED"))
                .andExpect(jsonPath("$.dietPdf").doesNotExist())
                .andExpect(jsonPath("$.emailError").doesNotExist());

        verify(planMailSender, never()).send(any(), any(), any());
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
    void shouldReturnUnavailableWhenPdfWritingThrowsARuntimeException() throws Exception {
        when(planService.generate("alice")).thenReturn(PlanResult.success(samplePlan(), 2000));
        when(planPdfWriter.dietPdf(any(), anyInt())).thenThrow(new IllegalArgumentException("The glyph is missing"));

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
        verify(planMailSender, never()).send(any(), any(), any());
    }

    @Test
    void shouldSendBothPdfsWhenSendEmailIsTrueAndAnAddressIsStored() throws Exception {
        final byte[] dietPdf = "%PDF-1.4 diet".getBytes(StandardCharsets.ISO_8859_1);
        final byte[] shoppingListPdf = "%PDF-1.4 list".getBytes(StandardCharsets.ISO_8859_1);
        stubSuccessfulPdfs(dietPdf, shoppingListPdf);
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(formWithEmail("alice@example.com")));
        when(planMailSender.send("alice@example.com", dietPdf, shoppingListPdf)).thenReturn(true);

        mockMvc.perform(post("/plan/generate").with(csrf()).param("sendEmail", "true").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.emailError").doesNotExist())
                .andExpect(jsonPath("$.dietPdf").value(Base64.getEncoder().encodeToString(dietPdf)));

        verify(planMailSender).send("alice@example.com", dietPdf, shoppingListPdf);
        verify(planMailSender, never()).send(eq("bob@example.com"), any(), any());
    }

    @Test
    void shouldReturnPdfsAndEmailErrorWhenTheMailerFails() throws Exception {
        final byte[] dietPdf = "%PDF-1.4 diet".getBytes(StandardCharsets.ISO_8859_1);
        final byte[] shoppingListPdf = "%PDF-1.4 list".getBytes(StandardCharsets.ISO_8859_1);
        stubSuccessfulPdfs(dietPdf, shoppingListPdf);
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(formWithEmail("alice@example.com")));
        when(planMailSender.send("alice@example.com", dietPdf, shoppingListPdf)).thenReturn(false);

        mockMvc.perform(post("/plan/generate").with(csrf()).param("sendEmail", "true").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.emailError").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.dietPdf").value(Base64.getEncoder().encodeToString(dietPdf)))
                .andExpect(jsonPath("$.shoppingListPdf").value(Base64.getEncoder().encodeToString(shoppingListPdf)));
    }

    @Test
    void shouldReturnPdfsAndEmailErrorWhenSendEmailIsTrueWithoutAnAddress() throws Exception {
        final byte[] dietPdf = "%PDF-1.4 diet".getBytes(StandardCharsets.ISO_8859_1);
        final byte[] shoppingListPdf = "%PDF-1.4 list".getBytes(StandardCharsets.ISO_8859_1);
        stubSuccessfulPdfs(dietPdf, shoppingListPdf);
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(ProfileFormDto.empty()));

        mockMvc.perform(post("/plan/generate").with(csrf()).param("sendEmail", "true").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.emailError").value("UNAVAILABLE"));

        verify(planMailSender, never()).send(any(), any(), any());
    }

    private void stubSuccessfulPdfs(final byte[] dietPdf, final byte[] shoppingListPdf) throws IOException {
        final WeekPlan weekPlan = samplePlan();
        when(planService.generate("alice")).thenReturn(PlanResult.success(weekPlan, 2000));
        when(planPdfWriter.dietPdf(weekPlan, 2000)).thenReturn(dietPdf);
        when(planPdfWriter.shoppingListPdf(weekPlan)).thenReturn(shoppingListPdf);
    }

    private ProfileFormDto formWithEmail(final String email) {
        return new ProfileFormDto(34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN,
                ActivityLevel.MODERATE, List.of(), List.of(), email);
    }

    private WeekPlan samplePlan() {
        return new WeekPlan(List.of(new DayMeals(
                meal("Żółć", List.of(new Ingredient("jądro", "100 g")), 400, 20, 10, 15, "Sparzyć żółć."),
                meal("Jogurt", List.of(new Ingredient("skyr", "200 g")), 200, 18, 8, 2, "Otwórz skyr."),
                meal("Schabowy", List.of(new Ingredient("schab", "150 g")), 800, 40, 50, 35, "Usmaż schab."),
                meal("Zupa", List.of(new Ingredient("warzywa", "300 g")), 600, 12, 40, 20,
                        "Gotuj warzywa do miękkości."))),
                List.of(
                        new ShoppingItem("Żółć", "100 g"),
                        new ShoppingItem("jądro", "100 g"),
                        new ShoppingItem("chleb", "60 g")));
    }

    private Meal meal(final String name,
                      final List<Ingredient> ingredients,
                      final int kcal,
                      final int protein,
                      final int carbs,
                      final int fat,
                      final String preparation) {
        return new Meal(name, preparation, List.of(new Portion("Ja", ingredients, kcal, protein, carbs, fat)));
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

        @Bean
        PlanMailSender planMailSender() {
            return mock(PlanMailSender.class);
        }

        @Bean
        ProfileService profileService() {
            return mock(ProfileService.class);
        }
    }
}
