package com.kenez92.plateplan.account.service;

import java.util.Optional;

import com.kenez92.plateplan.account.db.Account;
import com.kenez92.plateplan.account.db.AccountRepository;
import com.kenez92.plateplan.account.format.LoginNormalizer;
import com.kenez92.plateplan.account.model.RegistrationError;
import com.kenez92.plateplan.account.model.RegistrationResult;
import com.kenez92.plateplan.account.validator.RegistrationValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Creates an account. It queries the database only when a registration arrives. Expected outcomes
 * are returned as a {@link RegistrationResult}, not thrown. A database exception carries the login
 * in its message, so the message is never logged, kept, or passed on; only the class names of the
 * exception and its cause are logged, so a failure still leaves a trace.
 */
@Service
public class RegistrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationService.class);

    private static final String REFUSED_LOG = "Registration refused by the database: {} (cause: {})";
    private static final String FAILED_LOG = "Registration failed on the database: {} (cause: {})";

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationValidator registrationValidator;
    private final LoginNormalizer loginNormalizer;

    public RegistrationService(final AccountRepository accountRepository,
                               final PasswordEncoder passwordEncoder,
                               final RegistrationValidator registrationValidator,
                               final LoginNormalizer loginNormalizer) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.registrationValidator = registrationValidator;
        this.loginNormalizer = loginNormalizer;
    }

    public RegistrationResult register(final String username, final String password) {
        final String login = loginNormalizer.normalize(username);
        try {
            return registrationValidator.validate(login, password)
                    .or(() -> findTakenLogin(login))
                    .map(RegistrationResult::rejected)
                    .orElseGet(() -> RegistrationResult.created(createAccount(login, password)));
        } catch (final DataIntegrityViolationException exception) {
            LOGGER.info(REFUSED_LOG, exception.getClass().getName(), causeName(exception));
            return RegistrationResult.rejected(RegistrationError.LOGIN_TAKEN);
        } catch (final DataAccessException exception) {
            LOGGER.warn(FAILED_LOG, exception.getClass().getName(), causeName(exception));
            return RegistrationResult.rejected(RegistrationError.UNAVAILABLE);
        }
    }

    private Optional<RegistrationError> findTakenLogin(final String login) {
        return accountRepository.findByUsernameIgnoreCase(login).map(account -> RegistrationError.LOGIN_TAKEN);
    }

    private Account createAccount(final String login, final String password) {
        return accountRepository.save(new Account(login, passwordEncoder.encode(password)));
    }

    private String causeName(final DataAccessException exception) {
        return NestedExceptionUtils.getMostSpecificCause(exception).getClass().getName();
    }
}
