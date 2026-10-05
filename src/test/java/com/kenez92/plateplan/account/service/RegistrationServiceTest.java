package com.kenez92.plateplan.account.service;

import java.util.List;
import java.util.Optional;

import com.kenez92.plateplan.account.db.Account;
import com.kenez92.plateplan.account.db.AccountRepository;
import com.kenez92.plateplan.account.format.LoginNormalizer;
import com.kenez92.plateplan.account.model.RegistrationError;
import com.kenez92.plateplan.account.model.RegistrationResult;
import com.kenez92.plateplan.account.validator.RegistrationValidator;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
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
                accountRepository, passwordEncoder, new RegistrationValidator(), new LoginNormalizer());
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
                accountRepository, passwordEncoder, new RegistrationValidator(), new LoginNormalizer());
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
                accountRepository, passwordEncoder, new RegistrationValidator(), new LoginNormalizer());

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
                accountRepository, passwordEncoder, new RegistrationValidator(), new LoginNormalizer());

        final RegistrationResult actual = service.register(null, "correct horse");

        final RegistrationResult expected = RegistrationResult.rejected(RegistrationError.LOGIN_INVALID);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldRejectAnInvalidPasswordWithoutTouchingTheDatabase() {
        final AccountRepository accountRepository = mock(AccountRepository.class);
        final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        final RegistrationService service = new RegistrationService(
                accountRepository, passwordEncoder, new RegistrationValidator(), new LoginNormalizer());

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
                accountRepository, passwordEncoder, new RegistrationValidator(), new LoginNormalizer());
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
                accountRepository, passwordEncoder, new RegistrationValidator(), new LoginNormalizer());
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
                accountRepository, passwordEncoder, new RegistrationValidator(), new LoginNormalizer());
        when(accountRepository.findByUsernameIgnoreCase("alice"))
                .thenThrow(new DataAccessResourceFailureException("The database is unreachable"));

        final RegistrationResult actual = service.register("alice", "correct horse");

        final RegistrationResult expected = RegistrationResult.rejected(RegistrationError.UNAVAILABLE);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReportUnavailableWhenTheInsertFails() {
        final AccountRepository accountRepository = mock(AccountRepository.class);
        final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        final RegistrationService service = new RegistrationService(
                accountRepository, passwordEncoder, new RegistrationValidator(), new LoginNormalizer());
        when(accountRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("correct horse")).thenReturn("hashed-password");
        when(accountRepository.save(any(Account.class)))
                .thenThrow(new DataAccessResourceFailureException("The database is unreachable"));

        final RegistrationResult actual = service.register("alice", "correct horse");

        final RegistrationResult expected = RegistrationResult.rejected(RegistrationError.UNAVAILABLE);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldNormalizeTheLoginBeforeLookingItUp() {
        final AccountRepository accountRepository = mock(AccountRepository.class);
        final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        final RegistrationService service = new RegistrationService(
                accountRepository, passwordEncoder, new RegistrationValidator(), new LoginNormalizer());
        when(accountRepository.findByUsernameIgnoreCase("\u00e9ve")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("correct horse")).thenReturn("hashed-password");
        when(accountRepository.save(any(Account.class))).then(returnsFirstArg());

        final RegistrationResult actual = service.register("e\u0301ve", "correct horse");

        final RegistrationResult expected = RegistrationResult.created(new Account("\u00e9ve", "hashed-password"));
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldLogOnlyTheClassNamesWhenTheDatabaseFails() {
        final AccountRepository accountRepository = mock(AccountRepository.class);
        final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        final RegistrationService service = new RegistrationService(
                accountRepository, passwordEncoder, new RegistrationValidator(), new LoginNormalizer());
        when(accountRepository.findByUsernameIgnoreCase("alice"))
                .thenThrow(new DataAccessResourceFailureException("Key (login)=(alice) is unreachable"));
        final Logger logger = (Logger) LoggerFactory.getLogger(RegistrationService.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            service.register("alice", "correct horse");
        } finally {
            logger.detachAppender(appender);
        }

        final List<String> messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)).contains(DataAccessResourceFailureException.class.getName());
        assertThat(messages.get(0)).doesNotContain("alice", "unreachable", "correct horse");
    }
}