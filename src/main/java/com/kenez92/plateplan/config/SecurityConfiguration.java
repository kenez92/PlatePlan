package com.kenez92.plateplan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Denies every request unless it is listed as public. The login window is the home page, so a
 * signed-out request and a failed login both return there.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    private static final String LOGIN_WINDOW = "/";

    private static final String LOGIN_PROCESSING_URL = "/login";

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(requests -> requests
                        .requestMatchers(LOGIN_WINDOW, "/register", "/css/**", "/error").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage(LOGIN_WINDOW)
                        .loginProcessingUrl(LOGIN_PROCESSING_URL)
                        .usernameParameter("username")
                        .passwordParameter("password"));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
