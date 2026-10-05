package com.kenez92.plateplan.config;

import java.util.List;

import com.kenez92.plateplan.account.AccountPrincipalService;
import com.kenez92.plateplan.account.AccountSignInService;
import com.kenez92.plateplan.account.RegistrationService;
import com.kenez92.plateplan.controller.HomeController;
import com.kenez92.plateplan.controller.RegisterController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = {HomeController.class, RegisterController.class})
@Import({SecurityConfiguration.class, AccountSignInService.class, AccountPrincipalService.class})
class SecurityConfigurationTest {

    private final MockMvc mockMvc;

    @Autowired
    SecurityConfigurationTest(final MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void shouldServeThePublicPagesWithoutSignIn() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/register")).andExpect(status().isOk());
        mockMvc.perform(get("/css/site.css")).andExpect(status().isOk());
        mockMvc.perform(get("/js/profile-products.js")).andExpect(status().isOk());
        mockMvc.perform(get("/.well-known/appspecific/com.chrome.devtools.json")).andExpect(status().isOk());
    }

    @Test
    void shouldRedirectAnyOtherPathToTheLoginWindow() throws Exception {
        mockMvc.perform(get("/account/anything"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void shouldRejectALoginPostWithoutACsrfToken() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "anyone")
                        .param("password", "anything"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldSignInWithTheCorrectPassword() throws Exception {
        mockMvc.perform(formLogin("/login").user("alice").password("correct horse"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("alice"));
    }

    @Test
    void shouldSendAWrongPasswordBackToTheLoginWindow() throws Exception {
        mockMvc.perform(formLogin("/login").user("alice").password("wrong"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void shouldSendALoginBackToTheLoginWindowWhenTheLookupFails() throws Exception {
        mockMvc.perform(formLogin("/login").user("unreachable").password("correct horse"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void shouldSignOutAndReturnToTheLoginWindow() throws Exception {
        mockMvc.perform(post("/logout").with(user("alice")).with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"))
                .andExpect(unauthenticated());
    }

    @Test
    void shouldRedirectASignedOutVisitorFromTheProfileToTheLoginWindow() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void shouldRedirectASignedOutPostToTheProfileToTheLoginWindow() throws Exception {
        mockMvc.perform(post("/profile").with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void shouldRejectALogoutPostWithoutACsrfToken() throws Exception {
        mockMvc.perform(post("/logout").with(user("alice")))
                .andExpect(status().isForbidden());
    }

    /**
     * The register controller needs a registration service, which is not part of this web slice.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class RegistrationStub {

        @Bean
        RegistrationService registrationService() {
            return mock(RegistrationService.class);
        }
    }

    /**
     * Stands in for the database-backed lookup, which is not part of this web slice. The password
     * is hashed with the application's own encoder, and one login simulates an unreachable database.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class AccountLookupStub {

        @Bean
        UserDetailsService userDetailsService(final PasswordEncoder passwordEncoder) {
            final UserDetailsService accounts = new InMemoryUserDetailsManager(User.withUsername("alice")
                    .password(passwordEncoder.encode("correct horse"))
                    .authorities(List.of())
                    .build());
            return username -> {
                if ("unreachable".equals(username)) {
                    throw new DataAccessResourceFailureException("The database is unreachable");
                }
                return accounts.loadUserByUsername(username);
            };
        }
    }
}
