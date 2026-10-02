package com.kenez92.plateplan.account;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Creates an account. It queries the database only when a registration arrives. Expected outcomes
 * are returned as a {@link RegistrationResult}, not thrown. A database exception carries the login
 * in its message, so it is never logged, kept, or passed on.
 */
@Service
public class RegistrationService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationValidator registrationValidator;

    public RegistrationService(final AccountRepository accountRepository,
                               final PasswordEncoder passwordEncoder,
                               final RegistrationValidator registrationValidator) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.registrationValidator = registrationValidator;
    }

    public RegistrationResult register(final String username, final String password) {
        final String login = Optional.ofNullable(username).map(String::trim).orElse(StringUtils.EMPTY);
        try {
            return registrationValidator.validate(login, password)
                    .or(() -> findTakenLogin(login))
                    .map(RegistrationResult::rejected)
                    .orElseGet(() -> RegistrationResult.created(createAccount(login, password)));
        } catch (final DataIntegrityViolationException exception) {
            return RegistrationResult.rejected(RegistrationError.LOGIN_TAKEN);
        } catch (final DataAccessException exception) {
            return RegistrationResult.rejected(RegistrationError.UNAVAILABLE);
        }
    }

    private Optional<RegistrationError> findTakenLogin(final String login) {
        return accountRepository.findByUsernameIgnoreCase(login).map(account -> RegistrationError.LOGIN_TAKEN);
    }

    private Account createAccount(final String login, final String password) {
        return accountRepository.save(new Account(login, passwordEncoder.encode(password)));
    }
}
