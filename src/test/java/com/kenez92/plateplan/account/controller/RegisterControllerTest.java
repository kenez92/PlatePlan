package com.kenez92.plateplan.account.controller;

import com.kenez92.plateplan.account.controller.dto.RegistrationForm;
import com.kenez92.plateplan.account.db.Account;
import com.kenez92.plateplan.account.model.RegistrationError;
import com.kenez92.plateplan.account.model.RegistrationResult;
import com.kenez92.plateplan.account.service.AccountPrincipalService;
import com.kenez92.plateplan.account.service.AccountSignInService;
import com.kenez92.plateplan.account.service.RegistrationService;
import com.kenez92.plateplan.config.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(SpringExtension.class)
@WebMvcTest(RegisterController.class)
@Import({SecurityConfiguration.class, AccountSignInService.class, AccountPrincipalService.class})
class RegisterControllerTest {

    private final MockMvc mockMvc;

    @Autowired
    RegisterControllerTest(final MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void shouldShowTheRegistrationFormBelowTheLoginBar() throws Exception {
        final String html = mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains("Załóż konto", "Zaloguj się", "name=\"username\"");
        assertThat(html.indexOf("class=\"login\"")).isLessThan(html.indexOf("id=\"register-title\""));
    }

    @Test
    void shouldSignTheNewAccountInAndRedirectHome() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "alice")
                        .param("password", "correct horse"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("alice"));
    }

    @Test
    void shouldKeepTheSessionSignedInOnTheNextRequest() throws Exception {
        final MvcResult registration = mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "alice")
                        .param("password", "correct horse"))
                .andExpect(status().isFound())
                .andReturn();
        final MockHttpSession session = (MockHttpSession) registration.getRequest().getSession(false);

        mockMvc.perform(get("/register").session(session))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void shouldShowTheFormAgainWhenTheRegistrationFails() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "taken")
                        .param("password", "correct horse"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("error", RegistrationError.LOGIN_TAKEN.name()))
                .andExpect(model().attribute("registrationForm", new RegistrationForm("taken", "correct horse")))
                .andExpect(content().string(containsString("Login jest zajęty.")))
                .andExpect(unauthenticated());
    }

    @Test
    void shouldShowTheMessageForEachRegistrationError() throws Exception {
        final String invalidLogin = register("tiny", "correct horse");
        final String invalidPassword = register("alice", "short");
        final String takenLogin = register("taken", "correct horse");
        final String unavailable = register("offline", "correct horse");

        assertThat(invalidLogin).contains("role=\"alert\"", "Login musi mieć od 3 do 50 znaków.");
        assertThat(invalidPassword).contains("role=\"alert\"",
                "Hasło musi mieć co najmniej 8 znaków i nie więcej niż 72 bajty.");
        assertThat(takenLogin).contains("role=\"alert\"", "Login jest zajęty.");
        assertThat(unavailable).contains("role=\"alert\"", "Nie udało się założyć konta. Spróbuj ponownie za chwilę.");
    }

    @Test
    void shouldKeepTheTypedLoginInTheRegistrationForm() throws Exception {
        final String html = register("taken", "correct horse");

        assertThat(html).contains("value=\"taken\"");
        assertThat(html).doesNotContain("correct horse");
    }

    @Test
    void shouldEscapeTheTypedLoginInTheRegistrationForm() throws Exception {
        final String html = register("<b>x</b>", "correct horse");

        assertThat(html).contains("&lt;b&gt;x&lt;/b&gt;");
        assertThat(html).doesNotContain("<b>x</b>");
    }

    private String register(final String login, final String password) throws Exception {
        return mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", login)
                        .param("password", password))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    void shouldTreatMissingFieldsAsAnInvalidRegistration() throws Exception {
        mockMvc.perform(post("/register").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("error", RegistrationError.LOGIN_INVALID.name()))
                .andExpect(unauthenticated());
    }

    @Test
    void shouldNotEchoThePasswordBack() throws Exception {
        final MvcResult result = mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "taken")
                        .param("password", "correct horse"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("correct horse");
    }

    @Test
    void shouldRejectARegistrationPostWithoutACsrfToken() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "alice")
                        .param("password", "correct horse"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "bob")
    void shouldSendASignedInVisitorAwayFromTheRegistrationForm() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"));
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "taken")
                        .param("password", "correct horse"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"));
    }

    /**
     * Stands in for the database-backed registration, which is not part of this web slice. One login
     * is created, one is reported as taken.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class RegistrationStub {

        @Bean
        RegistrationService registrationService() {
            final RegistrationService registrationService = mock(RegistrationService.class);
            when(registrationService.register("alice", "correct horse"))
                    .thenReturn(RegistrationResult.created(new Account("alice", "stored-hash")));
            when(registrationService.register("taken", "correct horse"))
                    .thenReturn(RegistrationResult.rejected(RegistrationError.LOGIN_TAKEN));
            when(registrationService.register("tiny", "correct horse"))
                    .thenReturn(RegistrationResult.rejected(RegistrationError.LOGIN_INVALID));
            when(registrationService.register("alice", "short"))
                    .thenReturn(RegistrationResult.rejected(RegistrationError.PASSWORD_INVALID));
            when(registrationService.register("offline", "correct horse"))
                    .thenReturn(RegistrationResult.rejected(RegistrationError.UNAVAILABLE));
            when(registrationService.register("<b>x</b>", "correct horse"))
                    .thenReturn(RegistrationResult.rejected(RegistrationError.LOGIN_TAKEN));
            when(registrationService.register(null, null))
                    .thenReturn(RegistrationResult.rejected(RegistrationError.LOGIN_INVALID));
            return registrationService;
        }
    }
}
