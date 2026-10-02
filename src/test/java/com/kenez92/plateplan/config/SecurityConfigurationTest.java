package com.kenez92.plateplan.config;

import java.util.List;

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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = {HomeController.class, RegisterController.class})
@Import(SecurityConfiguration.class)
class SecurityConfigurationTest {

    private static final String PASSWORD = "correct horse";
    private static final String UNREACHABLE_LOGIN = "unreachable";

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
        mockMvc.perform(formLogin("/login").user("alice").password(PASSWORD))
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
        mockMvc.perform(formLogin("/login").user(UNREACHABLE_LOGIN).password(PASSWORD))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/?error"))
                .andExpect(unauthenticated());
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
                    .password(passwordEncoder.encode(PASSWORD))
                    .authorities(List.of())
                    .build());
            return username -> {
                if (UNREACHABLE_LOGIN.equals(username)) {
                    throw new DataAccessResourceFailureException("The database is unreachable");
                }
                return accounts.loadUserByUsername(username);
            };
        }
    }
}
