package com.kenez92.plateplan.config;

import com.kenez92.plateplan.controller.HomeController;
import com.kenez92.plateplan.controller.RegisterController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = {HomeController.class, RegisterController.class})
@Import(SecurityConfiguration.class)
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
}
