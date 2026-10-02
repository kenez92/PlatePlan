package com.kenez92.plateplan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Denies every request unless it is listed as public. The login window is the home page, so a
 * signed-out request and a failed login both return there.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    private static final String LOGIN_WINDOW = "/";
    private static final String REGISTER_PAGE = "/register";
    private static final String STYLESHEETS = "/css/**";
    private static final String ERROR_PAGE = "/error";
    private static final String HEALTH_ENDPOINT = "/actuator/health";
    private static final String INFO_ENDPOINT = "/actuator/info";
    private static final String LOGIN_PROCESSING_URL = "/login";
    private static final String USERNAME_PARAMETER = "username";
    private static final String PASSWORD_PARAMETER = "password";

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http,
                                                   final SecurityContextRepository securityContextRepository)
            throws Exception {
        http.securityContext(context -> context.securityContextRepository(securityContextRepository))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(LOGIN_WINDOW, REGISTER_PAGE, STYLESHEETS, ERROR_PAGE).permitAll()
                        .requestMatchers(HEALTH_ENDPOINT, INFO_ENDPOINT).permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage(LOGIN_WINDOW)
                        .loginProcessingUrl(LOGIN_PROCESSING_URL)
                        .usernameParameter(USERNAME_PARAMETER)
                        .passwordParameter(PASSWORD_PARAMETER))
                .logout(logout -> logout.logoutSuccessUrl(LOGIN_WINDOW));
        return http.build();
    }

    /**
     * The one place the signed-in state is stored between requests. The filter chain reads it and
     * the registration controller writes to it, so both must use this same bean.
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
