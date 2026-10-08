package com.kenez92.plateplan.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.web.RedirectStrategy;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoginFailureHandlerTest {

    @Test
    void shouldRedirectToUnavailableWhenTheLookupFails() throws Exception {
        final RedirectStrategy redirectStrategy = mock(RedirectStrategy.class);
        final LoginFailureHandler handler = new LoginFailureHandler(redirectStrategy);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        handler.onAuthenticationFailure(request, response, new InternalAuthenticationServiceException(
                "lookup", new DataAccessResourceFailureException("The database is unreachable")));

        verify(redirectStrategy).sendRedirect(request, response, "/?unavailable");
    }

    @Test
    void shouldRedirectToTheCredentialsErrorWhenThePasswordIsWrong() throws Exception {
        final RedirectStrategy redirectStrategy = mock(RedirectStrategy.class);
        final LoginFailureHandler handler = new LoginFailureHandler(redirectStrategy);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        handler.onAuthenticationFailure(request, response, new BadCredentialsException("bad password"));

        verify(redirectStrategy).sendRedirect(request, response, "/?error");
    }
}
