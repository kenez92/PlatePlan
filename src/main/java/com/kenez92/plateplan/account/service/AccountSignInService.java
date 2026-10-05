package com.kenez92.plateplan.account.service;

import com.kenez92.plateplan.account.db.Account;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Service;

/**
 * Signs an account in without the login form, as registration does. The form-login filter is
 * skipped on this path, so what it would do is done here: the session is renewed (against session
 * fixation), the CSRF token of the public form is dropped so a new one is issued, the password
 * hash is erased from the principal, and the security context is saved through the same repository
 * the filter chain reads. Nothing is logged.
 */
@Service
public class AccountSignInService {

    private final AccountPrincipalService accountPrincipalService;
    private final SecurityContextRepository securityContextRepository;
    private final CsrfTokenRepository csrfTokenRepository;

    public AccountSignInService(final AccountPrincipalService accountPrincipalService,
                                final SecurityContextRepository securityContextRepository,
                                final CsrfTokenRepository csrfTokenRepository) {
        this.accountPrincipalService = accountPrincipalService;
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    public void signIn(final Account account,
                       final HttpServletRequest request,
                       final HttpServletResponse response) {
        final User principal = accountPrincipalService.toUserDetails(account);
        principal.eraseCredentials();
        renewSession(request);
        csrfTokenRepository.saveToken(null, request, response);
        final SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    private void renewSession(final HttpServletRequest request) {
        if (request.getSession(false) != null) {
            request.changeSessionId();
        } else {
            request.getSession();
        }
    }
}
