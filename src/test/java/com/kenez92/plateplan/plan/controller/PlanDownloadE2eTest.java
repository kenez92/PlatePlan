package com.kenez92.plateplan.plan.controller;

import java.util.ArrayList;
import java.util.List;

import com.kenez92.plateplan.plan.model.DietPlan;
import com.kenez92.plateplan.plan.model.Ingredient;
import com.kenez92.plateplan.plan.model.Meal;
import com.kenez92.plateplan.plan.model.PlanResult;
import com.kenez92.plateplan.plan.model.ShoppingItem;
import com.kenez92.plateplan.plan.service.PlanService;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "DATABASE_URL=jdbc:postgresql://127.0.0.1:1/plateplan",
        "DATABASE_USERNAME=dummy",
        "DATABASE_PASSWORD=dummy",
        "SESSION_COOKIE_SECURE=false",
        "spring.main.allow-bean-definition-overriding=true"
})
class PlanDownloadE2eTest {

    private final int port;

    @Autowired
    PlanDownloadE2eTest(@LocalServerPort final int port) {
        this.port = port;
    }

    @Test
    void shouldOfferTwoNamedPdfDownloadsThatVanishOnRefresh() {
        try (Playwright playwright = Playwright.create();
                Browser browser = playwright.chromium().launch();
                BrowserContext browserContext = browser.newContext();
                Page page = browserContext.newPage()) {
            final List<Request> generatePosts = new ArrayList<>();
            page.onRequest(request -> {
                if ("POST".equalsIgnoreCase(request.method()) && request.url().contains("/plan/generate")) {
                    generatePosts.add(request);
                }
            });
            page.navigate("http://127.0.0.1:" + port + "/");
            page.locator("#login-username").fill("alice");
            page.locator("#login-password").fill("password1");
            page.locator("form.login button[type=submit]").click();
            page.locator(".signed-in-name").waitFor();
            page.navigate("http://127.0.0.1:" + port + "/plan");
            page.locator("#generate-plan").waitFor();
            final String csrfHeader = page.locator("#generate-plan").getAttribute("data-csrf-header");
            page.locator("#generate-plan").click();
            page.locator("#plan-downloads").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

            assertThat(csrfHeader).isNotBlank();
            assertThat(generatePosts).hasSize(1);
            assertThat(generatePosts.get(0).headers().get(csrfHeader.toLowerCase())).isNotBlank();
            assertThat(page.locator("#download-diet").getAttribute("download")).isEqualTo("dieta-na-jutro.pdf");
            assertThat(page.locator("#download-list").getAttribute("download")).isEqualTo("lista-zakupow.pdf");
            assertThat(page.locator("#download-diet").getAttribute("href")).startsWith("blob:");
            assertThat(page.locator("#download-list").getAttribute("href")).startsWith("blob:");

            page.reload();
            page.locator("#generate-plan").waitFor();
            assertThat(page.locator("#plan-downloads").isHidden()).isTrue();
            assertThat(page.locator("#download-diet").getAttribute("href")).isNull();
            assertThat(page.locator("#download-list").getAttribute("href")).isNull();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class BrowserCollaborators {

        @Bean(name = "accountUserDetailsService")
        @Primary
        UserDetailsService accountUserDetailsService(final PasswordEncoder passwordEncoder) {
            return new InMemoryUserDetailsManager(User.withUsername("alice")
                    .password(passwordEncoder.encode("password1"))
                    .authorities(List.of())
                    .build());
        }

        @Bean
        @Primary
        PlanService browserPlanService() {
            final PlanService planService = mock(PlanService.class);
            when(planService.generate("alice")).thenReturn(PlanResult.success(samplePlan(), 2000));
            return planService;
        }

        private DietPlan samplePlan() {
            return new DietPlan(
                    new Meal("Jajecznica", List.of(new Ingredient("jajka", "2 szt.")), 450, 28, 32, 22, "Usmaż jajka."),
                    new Meal("Jogurt", List.of(new Ingredient("jogurt", "200 g")), 250, 18, 30, 4, "Otwórz jogurt."),
                    new Meal("Schabowy", List.of(new Ingredient("schab", "150 g")), 800, 40, 55, 35, "Usmaż schab."),
                    new Meal("Zupa", List.of(new Ingredient("warzywa", "300 g")), 500, 12, 40, 18, "Gotuj warzywa."),
                    List.of(new ShoppingItem("jajka", "2 szt."), new ShoppingItem("jogurt", "200 g"),
                            new ShoppingItem("schab", "150 g"), new ShoppingItem("warzywa", "300 g")));
        }
    }
}
