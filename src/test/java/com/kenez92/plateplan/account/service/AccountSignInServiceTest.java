package com.kenez92.plateplan.account.service;

import java.util.List;

import com.kenez92.plateplan.account.db.Account;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountSignInServiceTest {

    @AfterEach
    void clearTheSecurityContextHolder() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSaveTheSignedInContextThroughTheRepository() {
        final SecurityContextRepository repository = mock(SecurityContextRepository.class);
        final AccountSignInService service = new AccountSignInService(
                new AccountPrincipalService(), repository, mock(CsrfTokenRepository.class));

        service.signIn(new Account("alice", "stored-hash"), new MockHttpServletRequest(),
                new MockHttpServletResponse());

        final ArgumentCaptor<SecurityContext> saved = ArgumentCaptor.forClass(SecurityContext.class);
        verify(repository).saveContext(saved.capture(), any(), any());
        assertThat(saved.getValue()).usingRecursiveComparison().isEqualTo(signedInContext());
    }

    @Test
    void shouldPutTheSignedInContextOnTheSecurityContextHolder() {
        final SecurityContextRepository repository = mock(SecurityContextRepository.class);
        final AccountSignInService service = new AccountSignInService(
                new AccountPrincipalService(), repository, mock(CsrfTokenRepository.class));

        service.signIn(new Account("alice", "stored-hash"), new MockHttpServletRequest(),
                new MockHttpServletResponse());

        assertThat(SecurityContextHolder.getContext()).usingRecursiveComparison().isEqualTo(signedInContext());
    }

    @Test
    void shouldNotKeepThePasswordHashOnThePrincipal() {
        final SecurityContextRepository repository = mock(SecurityContextRepository.class);
        final AccountSignInService service = new AccountSignInService(
                new AccountPrincipalService(), repository, mock(CsrfTokenRepository.class));

        service.signIn(new Account("alice", "stored-hash"), new MockHttpServletRequest(),
                new MockHttpServletResponse());

        final ArgumentCaptor<SecurityContext> saved = ArgumentCaptor.forClass(SecurityContext.class);
        verify(repository).saveContext(saved.capture(), any(), any());
        final User principal = (User) saved.getValue().getAuthentication().getPrincipal();
        assertThat(principal.getPassword()).isNull();
    }

    @Test
    void shouldDropTheCsrfTokenOfThePublicForm() {
        final CsrfTokenRepository csrfTokenRepository = mock(CsrfTokenRepository.class);
        final AccountSignInService service = new AccountSignInService(
                new AccountPrincipalService(), mock(SecurityContextRepository.class), csrfTokenRepository);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();

        service.signIn(new Account("alice", "stored-hash"), request, response);

        verify(csrfTokenRepository).saveToken(null, request, response);
    }

    @Test
    void shouldChangeTheSessionIdWhenASessionExists() {
        final SecurityContextRepository repository = mock(SecurityContextRepository.class);
        final AccountSignInService service = new AccountSignInService(
                new AccountPrincipalService(), repository, mock(CsrfTokenRepository.class));
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        final String idBefore = session.getId();

        service.signIn(new Account("alice", "stored-hash"), request, new MockHttpServletResponse());

        assertThat(request.getSession(false)).isSameAs(session);
        assertThat(session.getId()).isNotEqualTo(idBefore);
    }

    @Test
    void shouldStartASessionWhenThereIsNone() {
        final SecurityContextRepository repository = mock(SecurityContextRepository.class);
        final AccountSignInService service = new AccountSignInService(
                new AccountPrincipalService(), repository, mock(CsrfTokenRepository.class));
        final MockHttpServletRequest request = new MockHttpServletRequest();

        service.signIn(new Account("alice", "stored-hash"), request, new MockHttpServletResponse());

        assertThat(request.getSession(false)).isNotNull();
    }

    private SecurityContext signedInContext() {
        final User principal = new User("alice", "stored-hash", List.of());
        principal.eraseCredentials();
        return new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));
    }
}
