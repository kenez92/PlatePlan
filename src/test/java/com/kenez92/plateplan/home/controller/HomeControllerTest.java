package com.kenez92.plateplan.home.controller;

import com.kenez92.plateplan.config.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(HomeController.class)
@Import(SecurityConfiguration.class)
class HomeControllerTest {

    private final MockMvc mockMvc;

    @Autowired
    HomeControllerTest(final MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void shouldDescribeTheProductAndKeepLoginAtTheTop() throws Exception {
        final String html = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains(
                "Cały tydzień posiłków. Zero zgadywania.",
                "Załóż konto",
                "Co to jest PlatePlan",
                "Jak powstaje plan",
                "Kalorie i preferencje",
                "listę zakupów");
        assertThat(html).contains("href=\"/register\"", "name=\"username\"", "name=\"password\"",
                "href=\"/#produkt\"", "href=\"/#jak-dziala\"", "href=\"/#kalorie\"");
        assertThat(html.indexOf("class=\"login\"")).isLessThan(html.indexOf("id=\"tresc\""));
        assertThat(html).doesNotContain("Nieprawidłowy login lub hasło.",
                "Nie udało się zalogować. Spróbuj ponownie za chwilę.");
    }

    @Test
    void shouldShowTheLoginErrorMessageAfterAFailedLogin() throws Exception {
        final String html = mockMvc.perform(get("/?error"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains("role=\"alert\"", "Nieprawidłowy login lub hasło.");
        assertThat(html).doesNotContain("Nie udało się zalogować. Spróbuj ponownie za chwilę.");
    }

    @Test
    void shouldShowTheUnavailableMessageWhenLoginCannotReachTheDatabase() throws Exception {
        final String html = mockMvc.perform(get("/?unavailable"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains("role=\"alert\"", "Nie udało się zalogować. Spróbuj ponownie za chwilę.");
        assertThat(html).doesNotContain("Nieprawidłowy login lub hasło.");
    }

    @Test
    @WithMockUser(username = "alice")
    void shouldRedirectASignedInVisitorToThePlan() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/plan"));
    }

    @Test
    @WithMockUser(username = "<b>x</b>")
    void shouldRedirectASignedInVisitorWithoutWritingTheLogin() throws Exception {
        final String body = mockMvc.perform(get("/"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/plan"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain("<b>x</b>", "&lt;b&gt;x&lt;/b&gt;");
    }

}
