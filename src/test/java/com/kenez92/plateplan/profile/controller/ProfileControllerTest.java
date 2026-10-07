package com.kenez92.plateplan.profile.controller;

import java.math.BigDecimal;
import java.util.List;

import com.kenez92.plateplan.config.SecurityConfiguration;
import com.kenez92.plateplan.profile.controller.dto.ProfileFormDto;
import com.kenez92.plateplan.profile.model.ActivityLevel;
import com.kenez92.plateplan.profile.model.Goal;
import com.kenez92.plateplan.profile.model.ConfirmedCaloriesResult;
import com.kenez92.plateplan.profile.model.ProfileResult;
import com.kenez92.plateplan.profile.model.Sex;
import com.kenez92.plateplan.profile.service.ConfirmedCaloriesService;
import com.kenez92.plateplan.profile.service.ProfileService;
import com.kenez92.plateplan.profile.validator.ProductListsValidator;
import com.kenez92.plateplan.profile.validator.ProductNameValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(SpringExtension.class)
@WebMvcTest(ProfileController.class)
@Import({SecurityConfiguration.class, ProductListsValidator.class, ProductNameValidator.class})
@WithMockUser(username = "alice")
class ProfileControllerTest {

    private final MockMvc mockMvc;
    private final ProfileService profileService;
    private final ConfirmedCaloriesService confirmedCaloriesService;

    @Autowired
    ProfileControllerTest(final MockMvc mockMvc,
                          final ProfileService profileService,
                          final ConfirmedCaloriesService confirmedCaloriesService) {
        this.mockMvc = mockMvc;
        this.profileService = profileService;
        this.confirmedCaloriesService = confirmedCaloriesService;
    }

    @BeforeEach
    void resetTheService() {
        reset(profileService, confirmedCaloriesService);
        when(confirmedCaloriesService.load("alice")).thenReturn(ConfirmedCaloriesResult.loaded(null));
    }

    @Test
    void shouldShowAnEmptyFormWhenTheAccountHasNoProfile() throws Exception {
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(ProfileFormDto.empty()));

        final String html = mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains("Wiek", "Wzrost (cm)", "Waga (kg)", "Płeć", "Cel", "Poziom aktywności",
                "Cel kaloryczny (kcal)", "Zapisz cel", "Wylicz ponownie", "Produkty preferowane",
                "Produkty wykluczone", "Dodaj", "Zapisz profil", "Wybierz");
        assertThat(html).contains("action=\"/profile/calories\"", "action=\"/profile/recalculate\"");
        assertThat(html).doesNotContain("Profil zapisany.", "Nie udało się wczytać profilu",
                "Proponowane dzienne zapotrzebowanie");
    }

    @Test
    void shouldShowTheStoredProfile() throws Exception {
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(stored()));
        when(confirmedCaloriesService.load("alice")).thenReturn(ConfirmedCaloriesResult.loaded(2767));

        final String html = mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains("value=\"34\"", "value=\"180\"", "value=\"MALE\"", "value=\"MAINTAIN\"",
                "value=\"MODERATE\"", "value=\"mleko 3,2%\"", "value=\"jajka\"", "value=\"ser\"", "value=\"orzechy\"",
                "selected");
        assertThat(html).contains("82.5");
        assertThat(html).contains("Cel kaloryczny (kcal)", "value=\"2767\"", "Zapisz cel", "Wylicz ponownie");
        assertThat(html).doesNotContain("Proponowane dzienne zapotrzebowanie");
    }

    @Test
    void shouldShowAnEmptyCalorieFieldWhenNoneAreStored() throws Exception {
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(stored()));

        final String html = mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains("name=\"dailyCalories\"");
        assertThat(html).doesNotContain("value=\"2767\"");
    }

    @Test
    void shouldKeepTheEditedDailyCaloriesInsteadOfTheFormula() throws Exception {
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(stored()));
        when(confirmedCaloriesService.load("alice")).thenReturn(ConfirmedCaloriesResult.loaded(2000));

        final String html = mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains("value=\"2000\"");
        assertThat(html).doesNotContain("value=\"2767\"");
    }

    @Test
    void shouldEscapeEveryStoredAndTypedValue() throws Exception {
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(new ProfileFormDto(34, 180,
                new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN, ActivityLevel.MODERATE, List.of("jajka <b>x</b>"),
                List.of("ser <i>y</i>"))));

        final String html = mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains("jajka &lt;b&gt;x&lt;/b&gt;", "ser &lt;i&gt;y&lt;/i&gt;");
        assertThat(html).doesNotContain("<b>x</b>", "<i>y</i>");
    }

    @Test
    void shouldLoadAndSaveTheProfileOfTheSignedInLoginOnly() throws Exception {
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(ProfileFormDto.empty()));
        when(profileService.save(eq("alice"), any(ProfileFormDto.class))).thenReturn(ProfileResult.saved(stored()));

        mockMvc.perform(get("/profile").param("login", "bob"))
                .andExpect(status().isOk());
        mockMvc.perform(validPost().param("login", "bob"))
                .andExpect(status().isFound());

        verify(profileService).load("alice");
        final ArgumentCaptor<ProfileFormDto> saved = ArgumentCaptor.forClass(ProfileFormDto.class);
        verify(profileService).save(eq("alice"), saved.capture());
        assertThat(saved.getValue()).usingRecursiveComparison().isEqualTo(stored());
    }

    @Test
    void shouldRedirectWithASavedNoticeAfterASave() throws Exception {
        when(profileService.save(eq("alice"), any(ProfileFormDto.class))).thenReturn(ProfileResult.saved(stored()));
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(stored()));

        mockMvc.perform(validPost())
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("saved", true));

        final String html = mockMvc.perform(get("/profile").flashAttr("saved", true))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains("role=\"status\"", "Profil zapisany.");
        verify(confirmedCaloriesService, never()).update(any(), anyInt());
        verify(confirmedCaloriesService, never()).recalculate(any());
    }

    @Test
    void shouldShowTheFormAgainWithTheTypedValuesWhenTheSaveIsRefused() throws Exception {
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(ProfileFormDto.empty()));
        final String html = mockMvc.perform(post("/profile")
                        .with(csrf())
                        .param("age", "9")
                        .param("heightCm", "180")
                        .param("weightKg", "82.5")
                        .param("sex", "MALE")
                        .param("goal", "MAINTAIN")
                        .param("activityLevel", "MODERATE")
                        .param("preferredProducts", "mleko 3,2%")
                        .param("preferredProducts", "jajka")
                        .param("preferredProducts", "ser")
                        .param("excludedProducts", "orzechy"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        verify(profileService, never()).save(any(), any());
        assertThat(html).contains("value=\"9\"", "value=\"180\"", "value=\"mleko 3,2%\"", "value=\"jajka\"",
                "value=\"ser\"", "value=\"orzechy\"");
        assertThat(html).contains("Wiek: liczba całkowita od 10 do 110.");
    }

    @Test
    void shouldShowTheMessageForEachProblemNextToItsField() throws Exception {
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(ProfileFormDto.empty()));
        final String html = mockMvc.perform(post("/profile")
                        .with(csrf())
                        .param("age", "")
                        .param("heightCm", "79")
                        .param("weightKg", "72.55")
                        .param("sex", "MALE")
                        .param("goal", "MAINTAIN")
                        .param("activityLevel", "MODERATE")
                        .param("preferredProducts", "a")
                        .param("excludedProducts", ""))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        verify(profileService, never()).save(any(), any());
        assertThat(html).contains("role=\"alert\"", "Uzupełnij to pole.",
                "Wzrost: liczba całkowita od 80 do 250 cm.",
                "Waga: od 20 do 400 kg, najwyżej jedno miejsce po przecinku.",
                "Nazwa produktu musi mieć od 2 do 100 znaków");
    }

    @Test
    void shouldRefuseASaveWhenANameIsOnBothProductLists() throws Exception {
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(ProfileFormDto.empty()));
        final String html = mockMvc.perform(post("/profile")
                        .with(csrf())
                        .param("age", "34")
                        .param("heightCm", "180")
                        .param("weightKg", "82.5")
                        .param("sex", "MALE")
                        .param("goal", "MAINTAIN")
                        .param("activityLevel", "MODERATE")
                        .param("preferredProducts", "ser")
                        .param("excludedProducts", "Ser"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        verify(profileService, never()).save(any(), any());
        assertThat(html).contains("Ten produkt jest też na liście preferowanych");
    }

    @Test
    void shouldKeepTheTypedValuesWhenTheDatabaseFailsOnSave() throws Exception {
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(stored()));
        when(profileService.save(eq("alice"), any(ProfileFormDto.class)))
                .thenReturn(ProfileResult.unavailable(stored()));

        final String html = mockMvc.perform(validPost())
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains("Nie udało się zapisać profilu. Spróbuj ponownie za chwilę.",
                "value=\"34\"", "value=\"mleko 3,2%\"", "value=\"jajka\"", "value=\"ser\"", "value=\"orzechy\"");
        assertThat(html).doesNotContain("Nie udało się wczytać profilu");
    }

    @Test
    void shouldHideTheFormWhenTheProfileCannotBeLoaded() throws Exception {
        when(profileService.load("alice")).thenReturn(ProfileResult.unavailable(ProfileFormDto.empty()));

        final String html = mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains("Nie udało się wczytać profilu. Spróbuj ponownie za chwilę.");
        assertThat(html).doesNotContain("id=\"profile-age\"", "Zapisz profil");
    }

    @Test
    void shouldRejectAPostWithoutACsrfToken() throws Exception {
        mockMvc.perform(post("/profile")
                        .param("age", "34")
                        .param("heightCm", "180")
                        .param("weightKg", "82.5")
                        .param("sex", "MALE")
                        .param("goal", "MAINTAIN")
                        .param("activityLevel", "MODERATE")
                        .param("preferredProducts", "jajka")
                        .param("excludedProducts", ""))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldShowTheProfileLinkInTheHeaderOfASignedInVisitor() throws Exception {
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(ProfileFormDto.empty()));

        final String html = mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains("href=\"/profile\"", ">Profil<");
    }

    @Test
    void shouldSaveOnlyTheDailyCaloriesOnTheCalorieEndpoint() throws Exception {
        when(confirmedCaloriesService.update("alice", 2000)).thenReturn(ConfirmedCaloriesResult.saved(2000));

        mockMvc.perform(post("/profile/calories").with(csrf()).param("dailyCalories", "2000"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("calorieSaved", true));

        verify(confirmedCaloriesService).update("alice", 2000);
        verify(profileService, never()).save(any(), any());
    }

    @Test
    void shouldRefuseDailyCaloriesOutsideTheRangeOnTheCalorieEndpoint() throws Exception {
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(stored()));

        final String html = mockMvc.perform(post("/profile/calories").with(csrf()).param("dailyCalories", "799"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        verify(confirmedCaloriesService, never()).update(any(), anyInt());
        verify(profileService, never()).save(any(), any());
        assertThat(html).contains("Cel kaloryczny: liczba całkowita od 800 do 6000 kcal.");
    }

    @Test
    void shouldAskForASavedProfileBeforeEditingCalories() throws Exception {
        when(confirmedCaloriesService.update("alice", 2000))
                .thenReturn(ConfirmedCaloriesResult.noProfile());
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(ProfileFormDto.empty()));

        final String html = mockMvc.perform(post("/profile/calories").with(csrf()).param("dailyCalories", "2000"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains("Najpierw zapisz profil, potem możesz zmienić cel kaloryczny.");
        verify(profileService, never()).save(any(), any());
    }

    @Test
    void shouldRecalculateTheStoredDailyCaloriesWithoutTakingANumber() throws Exception {
        when(confirmedCaloriesService.recalculate("alice")).thenReturn(ConfirmedCaloriesResult.saved(2767));

        mockMvc.perform(post("/profile/recalculate").with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("calorieSaved", true));

        verify(confirmedCaloriesService).recalculate("alice");
        verify(profileService, never()).save(any(), any());
        verify(confirmedCaloriesService, never()).update(any(), anyInt());
    }

    @Test
    void shouldUpdateAndRecalculateTheCaloriesOfTheSignedInLoginOnly() throws Exception {
        when(confirmedCaloriesService.update("alice", 2000)).thenReturn(ConfirmedCaloriesResult.saved(2000));
        when(confirmedCaloriesService.recalculate("alice")).thenReturn(ConfirmedCaloriesResult.saved(2767));

        mockMvc.perform(post("/profile/calories").with(csrf()).param("dailyCalories", "2000").param("login", "bob"))
                .andExpect(status().isFound());
        mockMvc.perform(post("/profile/recalculate").with(csrf()).param("login", "bob"))
                .andExpect(status().isFound());

        verify(confirmedCaloriesService).update("alice", 2000);
        verify(confirmedCaloriesService).recalculate("alice");
        verify(confirmedCaloriesService, never()).update(eq("bob"), anyInt());
        verify(confirmedCaloriesService, never()).recalculate(eq("bob"));
    }

    @Test
    void shouldRejectACaloriePostWithoutACsrfToken() throws Exception {
        mockMvc.perform(post("/profile/calories").param("dailyCalories", "2000"))
                .andExpect(status().isForbidden());
    }

    private MockHttpServletRequestBuilder validPost() {
        return post("/profile")
                .with(csrf())
                .param("age", "34")
                .param("heightCm", "180")
                .param("weightKg", "82.5")
                .param("sex", "MALE")
                .param("goal", "MAINTAIN")
                .param("activityLevel", "MODERATE")
                .param("preferredProducts", "mleko 3,2%")
                .param("preferredProducts", "jajka")
                .param("preferredProducts", "ser")
                .param("excludedProducts", "orzechy");
    }

    private ProfileFormDto stored() {
        return new ProfileFormDto(34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN, ActivityLevel.MODERATE,
                List.of("mleko 3,2%", "jajka", "ser"), List.of("orzechy"));
    }

    /**
     * Stands in for the database-backed profile, which is not part of this web slice.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class ProfileServiceStub {

        @Bean
        ProfileService profileService() {
            return mock(ProfileService.class);
        }

        @Bean
        ConfirmedCaloriesService confirmedCaloriesService() {
            return mock(ConfirmedCaloriesService.class);
        }

        @Bean
        LocalValidatorFactoryBean validator() {
            return new LocalValidatorFactoryBean();
        }
    }
}
