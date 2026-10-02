package com.kenez92.plateplan.controller;

import com.kenez92.plateplan.account.Account;
import com.kenez92.plateplan.account.AccountPrincipalService;
import com.kenez92.plateplan.account.AccountSignInService;
import com.kenez92.plateplan.account.RegistrationError;
import com.kenez92.plateplan.account.RegistrationResult;
import com.kenez92.plateplan.account.RegistrationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import com.kenez92.plateplan.config.SecurityConfiguration;
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
                .andExpect(content().string(containsString("Ten login jest już zajęty.")))
                .andExpect(unauthenticated());
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
            when(registrationService.register(null, null))
                    .thenReturn(RegistrationResult.rejected(RegistrationError.LOGIN_INVALID));
            return registrationService;
        }
    }
}
