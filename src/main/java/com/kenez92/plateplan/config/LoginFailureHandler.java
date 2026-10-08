package com.kenez92.plateplan.config;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/**
 * Sends a failed password back to the login window as a credentials error, and a lookup that
 * could not reach the database as an outage. The user copy stays generic; only class names are
 * logged.
 */
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginFailureHandler.class);

    private static final String LOGIN_WINDOW = "/";
    private static final String QUERY_PREFIX = "?";
    private static final String ERROR_FLAG = "error";
    private static final String UNAVAILABLE_FLAG = "unavailable";
    private static final String CREDENTIALS_FAILURE = LOGIN_WINDOW + QUERY_PREFIX + ERROR_FLAG;
    private static final String UNAVAILABLE_FAILURE = LOGIN_WINDOW + QUERY_PREFIX + UNAVAILABLE_FLAG;
    private static final String FAILED_LOG = "Login failed on the database: {} (cause: {})";

    private final RedirectStrategy redirectStrategy;

    public LoginFailureHandler(final RedirectStrategy redirectStrategy) {
        this.redirectStrategy = redirectStrategy;
    }

    @Override
    public void onAuthenticationFailure(final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        final AuthenticationException exception)
            throws IOException {
        if (exception instanceof InternalAuthenticationServiceException) {
            logFailure(exception);
            redirectStrategy.sendRedirect(request, response, UNAVAILABLE_FAILURE);
            return;
        }
        redirectStrategy.sendRedirect(request, response, CREDENTIALS_FAILURE);
    }

    private void logFailure(final AuthenticationException exception) {
        LOGGER.warn(FAILED_LOG, exception.getClass().getName(),
                NestedExceptionUtils.getMostSpecificCause(exception).getClass().getName());
    }
}
