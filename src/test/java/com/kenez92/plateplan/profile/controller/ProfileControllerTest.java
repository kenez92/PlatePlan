package com.kenez92.plateplan.profile.controller;

import java.math.BigDecimal;
import java.util.List;

import com.kenez92.plateplan.config.SecurityConfiguration;
import com.kenez92.plateplan.profile.controller.dto.HouseholdMemberForm;
import com.kenez92.plateplan.profile.controller.dto.ProfileFormDto;
import com.kenez92.plateplan.profile.db.HouseholdMember;
import com.kenez92.plateplan.profile.format.ProductListFormat;
import com.kenez92.plateplan.profile.model.ActivityLevel;
import com.kenez92.plateplan.profile.model.Goal;
import com.kenez92.plateplan.profile.model.ConfirmedCaloriesResult;
import com.kenez92.plateplan.profile.model.HouseholdMemberError;
import com.kenez92.plateplan.profile.model.HouseholdMemberResult;
import com.kenez92.plateplan.profile.model.ProfileResult;
import com.kenez92.plateplan.profile.model.Sex;
import com.kenez92.plateplan.profile.service.ConfirmedCaloriesService;
import com.kenez92.plateplan.profile.service.HouseholdMemberService;
import com.kenez92.plateplan.profile.service.ProfileService;
import com.kenez92.plateplan.profile.validator.EmailValidator;
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
import static org.mockito.ArgumentMatchers.anyLong;
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
@Import({SecurityConfiguration.class, ProductListsValidator.class, ProductNameValidator.class,
        EmailValidator.class, ProductListFormat.class})
@WithMockUser(username = "alice")
class ProfileControllerTest {

    private final MockMvc mockMvc;
    private final ProfileService profileService;
    private final ConfirmedCaloriesService confirmedCaloriesService;
    private final HouseholdMemberService householdMemberService;

    @Autowired
    ProfileControllerTest(final MockMvc mockMvc,
                          final ProfileService profileService,
                          final ConfirmedCaloriesService confirmedCaloriesService,
                          final HouseholdMemberService householdMemberService) {
        this.mockMvc = mockMvc;
        this.profileService = profileService;
        this.confirmedCaloriesService = confirmedCaloriesService;
        this.householdMemberService = householdMemberService;
    }

    @BeforeEach
    void resetTheService() {
        reset(profileService, confirmedCaloriesService, householdMemberService);
        when(confirmedCaloriesService.load("alice")).thenReturn(ConfirmedCaloriesResult.loaded(null));
        when(householdMemberService.list("alice")).thenReturn(HouseholdMemberResult.listed(List.of()));
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
                "Cel kaloryczny (kcal)", "Zapisz cel", "Wylicz ponownie", "E-mail do wysyłki planu",
                "Produkty preferowane",
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
        verify(householdMemberService, never()).create(any(), any());
        verify(householdMemberService, never()).replaceBody(any(), anyLong(), any());
    }

    @Test
    void shouldShowTheFormAgainWithTheTypedValuesWhenTheSaveIsRefused() throws Exception {
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(ProfileFormDto.empty()));
        final String html = mockMvc.perform(post("/profile")
                        .with(csrf())
                        .param("age", "1")
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
        assertThat(html).contains("value=\"1\"", "value=\"180\"", "value=\"mleko 3,2%\"", "value=\"jajka\"",
                "value=\"ser\"", "value=\"orzechy\"");
        assertThat(html).contains("Wiek: liczba całkowita od 2 do 110.");
    }

    @Test
    void shouldShowTheMessageForEachProblemNextToItsField() throws Exception {
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(ProfileFormDto.empty()));
        final String html = mockMvc.perform(post("/profile")
                        .with(csrf())
                        .param("age", "")
                        .param("heightCm", "69")
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
                "Wzrost: liczba całkowita od 70 do 250 cm.",
                "Waga: od 8 do 400 kg, najwyżej jedno miejsce po przecinku.",
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
    void shouldRefuseASaveWhenTheEmailIsInvalid() throws Exception {
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(ProfileFormDto.empty()));
        when(confirmedCaloriesService.load("alice")).thenReturn(ConfirmedCaloriesResult.loaded(null));

        final String html = mockMvc.perform(validPost().param("email", "not-an-address"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        verify(profileService, never()).save(any(), any());
        assertThat(html).contains("Podaj poprawny adres e-mail.");
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
    void shouldHideTheFormWhenCaloriesCannotBeLoaded() throws Exception {
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(stored()));
        when(confirmedCaloriesService.load("alice")).thenReturn(ConfirmedCaloriesResult.failed());

        final String html = mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains("Nie udało się wczytać profilu. Spróbuj ponownie za chwilę.");
        assertThat(html).doesNotContain("id=\"profile-age\"", "Zapisz profil");
    }

    @Test
    void shouldHideTheFormWhenCaloriesCannotBeLoadedAfterAFailedSave() throws Exception {
        when(profileService.save(eq("alice"), any(ProfileFormDto.class)))
                .thenReturn(ProfileResult.unavailable(stored()));
        when(confirmedCaloriesService.load("alice")).thenReturn(ConfirmedCaloriesResult.failed());

        final String html = mockMvc.perform(validPost())
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains("Nie udało się zapisać profilu. Spróbuj ponownie za chwilę.",
                "Nie udało się wczytać profilu. Spróbuj ponownie za chwilę.");
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

        final String html = mockMvc.perform(post("/profile/calories").with(csrf()).param("dailyCalories", "399"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        verify(confirmedCaloriesService, never()).update(any(), anyInt());
        verify(profileService, never()).save(any(), any());
        assertThat(html).contains("Cel kaloryczny: liczba całkowita od 400 do 6000 kcal.");
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

    @Test
    void shouldRejectARecalculatePostWithoutACsrfToken() throws Exception {
        mockMvc.perform(post("/profile/recalculate"))
                .andExpect(status().isForbidden());

        verify(confirmedCaloriesService, never()).recalculate(any());
    }

    @Test
    void shouldShowJaWhenTheOwnerNameIsEmpty() throws Exception {
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(stored()));

        final String html = mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains(">Ja<", "Puste pole zostawia podpis Ja.", "Dodane osoby", "Nowa osoba",
                "action=\"/profile/members\"");
    }

    @Test
    void shouldSaveTheOwnerDisplayNameWithoutSavingAMember() throws Exception {
        when(profileService.save(eq("alice"), any(ProfileFormDto.class))).thenReturn(ProfileResult.saved(stored()));

        mockMvc.perform(validPost().param("displayName", "Daniel"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/profile"));

        final ArgumentCaptor<ProfileFormDto> saved = ArgumentCaptor.forClass(ProfileFormDto.class);
        verify(profileService).save(eq("alice"), saved.capture());
        assertThat(saved.getValue()).usingRecursiveComparison().isEqualTo(new ProfileFormDto(34, 180,
                new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN, ActivityLevel.MODERATE,
                List.of("mleko 3,2%", "jajka", "ser"), List.of("orzechy"), null, "Daniel"));
        verify(householdMemberService, never()).create(any(), any());
        verify(householdMemberService, never()).replaceBody(any(), anyLong(), any());
    }

    @Test
    void shouldShowAMemberOnItsOwnForm() throws Exception {
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(stored()));
        when(householdMemberService.list("alice")).thenReturn(HouseholdMemberResult.listed(List.of(member(4L, "Ada", 795,
                "mleko"))));

        final String html = mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains(">Ada<", "value=\"795\"", "value=\"mleko\"", "Usuń osobę", "data-product-list",
                "action=\"/profile/members/4\"", "action=\"/profile/members/4/calories\"",
                "action=\"/profile/members/4/recalculate\"", "action=\"/profile/members/4/delete\"");
    }

    @Test
    void shouldSaveAMemberWithoutSavingTheOwner() throws Exception {
        when(householdMemberService.create(eq("alice"), any(HouseholdMemberForm.class)))
                .thenReturn(HouseholdMemberResult.saved(member(4L, "Ada", 795, null)));

        mockMvc.perform(memberPost("/profile/members").param("login", "bob"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("memberSaved", true));

        final ArgumentCaptor<HouseholdMemberForm> saved = ArgumentCaptor.forClass(HouseholdMemberForm.class);
        verify(householdMemberService).create(eq("alice"), saved.capture());
        assertThat(saved.getValue()).usingRecursiveComparison().isEqualTo(memberForm("Ada"));
        verify(householdMemberService, never()).create(eq("bob"), any());
        verify(profileService, never()).save(any(), any());
    }

    @Test
    void shouldNotSaveTheOwnerWhenTheMemberNameIsTaken() throws Exception {
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(stored()));
        when(householdMemberService.create(eq("alice"), any(HouseholdMemberForm.class)))
                .thenReturn(HouseholdMemberResult.rejected(HouseholdMemberError.NAME_TAKEN));

        final String html = mockMvc.perform(memberPost("/profile/members", "ja"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        verify(profileService, never()).save(any(), any());
        verify(householdMemberService).create(eq("alice"), any(HouseholdMemberForm.class));
        assertThat(html).contains("Ta nazwa jest już używana.", "value=\"ja\"");
    }

    @Test
    void shouldKeepTheMemberFormWhenTheCalorieResultIsRefused() throws Exception {
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(stored()));
        when(householdMemberService.create(eq("alice"), any(HouseholdMemberForm.class)))
                .thenReturn(HouseholdMemberResult.rejected(HouseholdMemberError.CALORIES));

        final String html = mockMvc.perform(memberPost("/profile/members"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        verify(profileService, never()).save(any(), any());
        verify(householdMemberService, never()).updateCalories(any(), anyLong(), anyInt());
        assertThat(html).contains("Wynik jest poza zakresem od 400 do 6000 kcal.", "value=\"Ada\"", "value=\"2\"");
    }

    @Test
    void shouldShowTheLimitAndKeepTheThreeMembers() throws Exception {
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(stored()));
        when(householdMemberService.list("alice")).thenReturn(HouseholdMemberResult.listed(List.of(
                member(1L, "Anna", 2000, null), member(2L, "Bartek", 2100, null), member(3L, "Celina", 2200, null))));
        when(householdMemberService.create(eq("alice"), any(HouseholdMemberForm.class)))
                .thenReturn(HouseholdMemberResult.rejected(HouseholdMemberError.LIMIT));

        final String html = mockMvc.perform(memberPost("/profile/members", "Dawid"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        verify(profileService, never()).save(any(), any());
        assertThat(html).contains("Możesz dodać najwyżej trzy osoby.", ">Anna<", ">Bartek<", ">Celina<");
        assertThat(html).doesNotContain("Dawid", "Nowa osoba");
    }

    @Test
    void shouldNotPassAnotherLoginToMemberSaveOrDelete() throws Exception {
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(stored()));
        when(householdMemberService.list("alice"))
                .thenReturn(HouseholdMemberResult.listed(List.of(member(4L, "Ada", 795, null))));
        when(householdMemberService.replaceBody(eq("alice"), eq(9L), any(HouseholdMemberForm.class)))
                .thenReturn(HouseholdMemberResult.rejected(HouseholdMemberError.NOT_FOUND));
        when(householdMemberService.delete("alice", 9L))
                .thenReturn(HouseholdMemberResult.rejected(HouseholdMemberError.NOT_FOUND));

        final String saved = mockMvc.perform(memberPost("/profile/members/9").param("login", "bob"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        final String deleted = mockMvc.perform(post("/profile/members/9/delete").with(csrf()).param("login", "bob"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        verify(householdMemberService).replaceBody(eq("alice"), eq(9L), any(HouseholdMemberForm.class));
        verify(householdMemberService).delete("alice", 9L);
        verify(householdMemberService, never()).replaceBody(eq("bob"), anyLong(), any());
        verify(householdMemberService, never()).delete(eq("bob"), anyLong());
        verify(householdMemberService, never()).updateCalories(eq("bob"), anyLong(), anyInt());
        verify(profileService, never()).save(any(), any());
        assertThat(saved).contains("Nie znaleziono osoby.", ">Ada<");
        assertThat(deleted).contains("Nie znaleziono osoby.", ">Ada<");
        assertThat(saved).doesNotContain("bob@example.com");
        assertThat(deleted).doesNotContain("bob@example.com");
    }

    @Test
    void shouldRefuseMemberCaloriesOutsideTheRange() throws Exception {
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(stored()));
        when(householdMemberService.list("alice"))
                .thenReturn(HouseholdMemberResult.listed(List.of(member(4L, "Ada", 795, null))));

        final String html = mockMvc.perform(post("/profile/members/4/calories").with(csrf()).param("dailyCalories", "399"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        verify(householdMemberService, never()).updateCalories(any(), anyLong(), anyInt());
        verify(profileService, never()).save(any(), any());
        assertThat(html).contains("Cel kaloryczny: liczba całkowita od 400 do 6000 kcal.", "value=\"399\"");
    }

    @Test
    void shouldKeepTheStoredMemberCaloriesWhenRecalculateIsRefused() throws Exception {
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(stored()));
        when(householdMemberService.list("alice"))
                .thenReturn(HouseholdMemberResult.listed(List.of(member(4L, "Ada", 795, null))));
        when(householdMemberService.recalculate("alice", 4L))
                .thenReturn(HouseholdMemberResult.rejected(HouseholdMemberError.CALORIES));

        final String html = mockMvc.perform(post("/profile/members/4/recalculate").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        verify(householdMemberService).recalculate("alice", 4L);
        verify(householdMemberService, never()).updateCalories(any(), anyLong(), anyInt());
        verify(profileService, never()).save(any(), any());
        assertThat(html).contains("Wynik jest poza zakresem od 400 do 6000 kcal.", "value=\"795\"");
    }

    @Test
    void shouldShowAMemberFieldErrorWithoutSavingTheOwner() throws Exception {
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(stored()));
        when(householdMemberService.list("alice"))
                .thenReturn(HouseholdMemberResult.listed(List.of(member(4L, "Ada", 795, null))));

        final String html = mockMvc.perform(post("/profile/members/4")
                        .with(csrf())
                        .param("displayName", "Ada")
                        .param("age", "1")
                        .param("heightCm", "70")
                        .param("weightKg", "8.5")
                        .param("sex", "FEMALE")
                        .param("goal", "GAIN")
                        .param("activityLevel", "MODERATE"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        verify(householdMemberService, never()).replaceBody(any(), anyLong(), any());
        verify(profileService, never()).save(any(), any());
        assertThat(html).contains("Wiek: liczba całkowita od 2 do 110.", "value=\"1\"", ">Ada<");
    }

    @Test
    void shouldRejectAMemberPostWithoutACsrfToken() throws Exception {
        mockMvc.perform(post("/profile/members")
                        .param("displayName", "Ada")
                        .param("age", "2")
                        .param("heightCm", "70")
                        .param("weightKg", "8.5")
                        .param("sex", "FEMALE")
                        .param("goal", "GAIN")
                        .param("activityLevel", "MODERATE"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/profile/members/4/delete"))
                .andExpect(status().isForbidden());

        verify(householdMemberService, never()).create(any(), any());
        verify(householdMemberService, never()).delete(any(), anyLong());
    }

    @Test
    void shouldKeepTheLoadFailureWhenMembersCannotBeListed() throws Exception {
        when(profileService.load("alice")).thenReturn(ProfileResult.loaded(stored()));
        when(householdMemberService.list("alice"))
                .thenReturn(HouseholdMemberResult.rejected(HouseholdMemberError.UNAVAILABLE));

        final String html = mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains("Nie udało się wczytać profilu. Spróbuj ponownie za chwilę.");
        assertThat(html).doesNotContain("id=\"profile-age\"", "Zapisz profil", "Dodane osoby");
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

    private MockHttpServletRequestBuilder memberPost(final String path) {
        return memberPost(path, "Ada");
    }

    private MockHttpServletRequestBuilder memberPost(final String path, final String displayName) {
        return post(path)
                .with(csrf())
                .param("displayName", displayName)
                .param("age", "2")
                .param("heightCm", "70")
                .param("weightKg", "8.5")
                .param("sex", "FEMALE")
                .param("goal", "GAIN")
                .param("activityLevel", "MODERATE");
    }

    private HouseholdMemberForm memberForm(final String displayName) {
        return new HouseholdMemberForm(displayName, 2, 70, new BigDecimal("8.5"), Sex.FEMALE, Goal.GAIN,
                ActivityLevel.MODERATE, List.of(), List.of());
    }

    private HouseholdMember member(final long memberId, final String displayName, final int calories,
                                   final String preferred) {
        return new HouseholdMember(memberId, "alice", displayName, 2, 70, new BigDecimal("8.5"), Sex.FEMALE,
                Goal.GAIN, ActivityLevel.MODERATE, calories, preferred, null);
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
        HouseholdMemberService householdMemberService() {
            return mock(HouseholdMemberService.class);
        }

        @Bean
        LocalValidatorFactoryBean validator() {
            return new LocalValidatorFactoryBean();
        }
    }
}
