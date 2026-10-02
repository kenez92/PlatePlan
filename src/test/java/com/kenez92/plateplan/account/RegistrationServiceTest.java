package com.kenez92.plateplan.account;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Test
    void shouldCreateTheAccountWithAHashedPasswordAndTheTrimmedLogin() {
        final AccountRepository accountRepository = mock(AccountRepository.class);
        final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        final RegistrationService service = new RegistrationService(
                accountRepository, passwordEncoder, new RegistrationValidator());
        when(accountRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("correct horse")).thenReturn("hashed-password");
        when(accountRepository.save(any(Account.class))).then(returnsFirstArg());

        final RegistrationResult actual = service.register("  alice  ", "correct horse");

        final RegistrationResult expected = RegistrationResult.created(new Account("alice", "hashed-password"));
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldKeepTheLetterCaseTheUserTyped() {
        final AccountRepository accountRepository = mock(AccountRepository.class);
        final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        final RegistrationService service = new RegistrationService(
                accountRepository, passwordEncoder, new RegistrationValidator());
        when(accountRepository.findByUsernameIgnoreCase("AlIcE")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("correct horse")).thenReturn("hashed-password");
        when(accountRepository.save(any(Account.class))).then(returnsFirstArg());

        final RegistrationResult actual = service.register("AlIcE", "correct horse");

        final RegistrationResult expected = RegistrationResult.created(new Account("AlIcE", "hashed-password"));
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldRejectAnInvalidLoginWithoutTouchingTheDatabase() {
        final AccountRepository accountRepository = mock(AccountRepository.class);
        final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        final RegistrationService service = new RegistrationService(
                accountRepository, passwordEncoder, new RegistrationValidator());

        final RegistrationResult actual = service.register("  ab  ", "correct horse");

        final RegistrationResult expected = RegistrationResult.rejected(RegistrationError.LOGIN_INVALID);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
        verify(accountRepository, never()).findByUsernameIgnoreCase(any());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void shouldRejectAMissingLogin() {
        final AccountRepository accountRepository = mock(AccountRepository.class);
        final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        final RegistrationService service = new RegistrationService(
                accountRepository, passwordEncoder, new RegistrationValidator());

        final RegistrationResult actual = service.register(null, "correct horse");

        final RegistrationResult expected = RegistrationResult.rejected(RegistrationError.LOGIN_INVALID);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldRejectAnInvalidPasswordWithoutTouchingTheDatabase() {
        final AccountRepository accountRepository = mock(AccountRepository.class);
        final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        final RegistrationService service = new RegistrationService(
                accountRepository, passwordEncoder, new RegistrationValidator());

        final RegistrationResult actual = service.register("alice", "1234567");

        final RegistrationResult expected = RegistrationResult.rejected(RegistrationError.PASSWORD_INVALID);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
        verify(accountRepository, never()).findByUsernameIgnoreCase(any());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void shouldRejectATakenLoginIgnoringCase() {
        final AccountRepository accountRepository = mock(AccountRepository.class);
        final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        final RegistrationService service = new RegistrationService(
                accountRepository, passwordEncoder, new RegistrationValidator());
        when(accountRepository.findByUsernameIgnoreCase("ALICE"))
                .thenReturn(Optional.of(new Account("alice", "hashed-password")));

        final RegistrationResult actual = service.register("ALICE", "correct horse");

        final RegistrationResult expected = RegistrationResult.rejected(RegistrationError.LOGIN_TAKEN);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void shouldReportTheLoginAsTakenWhenTheInsertHitsTheUniqueIndex() {
        final AccountRepository accountRepository = mock(AccountRepository.class);
        final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        final RegistrationService service = new RegistrationService(
                accountRepository, passwordEncoder, new RegistrationValidator());
        when(accountRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("correct horse")).thenReturn("hashed-password");
        when(accountRepository.save(any(Account.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        final RegistrationResult actual = service.register("alice", "correct horse");

        final RegistrationResult expected = RegistrationResult.rejected(RegistrationError.LOGIN_TAKEN);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReportUnavailableWhenTheDatabaseFails() {
        final AccountRepository accountRepository = mock(AccountRepository.class);
        final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        final RegistrationService service = new RegistrationService(
                accountRepository, passwordEncoder, new RegistrationValidator());
        when(accountRepository.findByUsernameIgnoreCase("alice"))
                .thenThrow(new DataAccessResourceFailureException("The database is unreachable"));

        final RegistrationResult actual = service.register("alice", "correct horse");

        final RegistrationResult expected = RegistrationResult.rejected(RegistrationError.UNAVAILABLE);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
}
