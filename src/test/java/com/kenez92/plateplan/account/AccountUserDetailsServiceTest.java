package com.kenez92.plateplan.account;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountUserDetailsServiceTest {

    @Test
    void shouldReturnUserDetailsWhenTheAccountExists() {
        final AccountRepository accountRepository = mock(AccountRepository.class);
        final AccountUserDetailsService service = new AccountUserDetailsService(
                accountRepository, new AccountPrincipalService(), new LoginNormalizer());
        when(accountRepository.findByUsernameIgnoreCase("ALICE"))
                .thenReturn(Optional.of(new Account("alice", "stored-hash")));

        final UserDetails actual = service.loadUserByUsername("ALICE");

        final UserDetails expected = User.withUsername("alice")
                .password("stored-hash")
                .authorities(List.of())
                .build();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldThrowWhenTheAccountDoesNotExist() {
        final AccountRepository accountRepository = mock(AccountRepository.class);
        final AccountUserDetailsService service = new AccountUserDetailsService(
                accountRepository, new AccountPrincipalService(), new LoginNormalizer());
        when(accountRepository.findByUsernameIgnoreCase("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nobody"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void shouldIgnoreWhiteSpaceAroundTheLogin() {
        final AccountRepository accountRepository = mock(AccountRepository.class);
        final AccountUserDetailsService service = new AccountUserDetailsService(
                accountRepository, new AccountPrincipalService(), new LoginNormalizer());
        when(accountRepository.findByUsernameIgnoreCase("alice"))
                .thenReturn(Optional.of(new Account("alice", "stored-hash")));

        final UserDetails actual = service.loadUserByUsername("  alice ");

        final UserDetails expected = User.withUsername("alice")
                .password("stored-hash")
                .authorities(List.of())
                .build();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
}