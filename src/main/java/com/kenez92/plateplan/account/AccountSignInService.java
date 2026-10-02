package com.kenez92.plateplan.account;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

/**
 * Signs an account in without the login form, as registration does. The form-login filter is
 * skipped on this path, so the session is renewed here (against session fixation) and the security
 * context is saved through the same repository the filter chain reads. Nothing is logged.
 */
@Service
public class AccountSignInService {

    private final AccountPrincipalService accountPrincipalService;
    private final SecurityContextRepository securityContextRepository;

    public AccountSignInService(final AccountPrincipalService accountPrincipalService,
                                final SecurityContextRepository securityContextRepository) {
        this.accountPrincipalService = accountPrincipalService;
        this.securityContextRepository = securityContextRepository;
    }

    public void signIn(final Account account,
                       final HttpServletRequest request,
                       final HttpServletResponse response) {
        final UserDetails principal = accountPrincipalService.toUserDetails(account);
        renewSession(request);
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
