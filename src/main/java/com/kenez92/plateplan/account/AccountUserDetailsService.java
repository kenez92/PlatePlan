package com.kenez92.plateplan.account;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Lets Spring Security sign a person in against the account table. It queries only when a login
 * arrives, never at start, so the application still starts without a database. The login is
 * normalized as at registration, so a stray space around it does not lock the person out. Nothing
 * about the account is logged here.
 */
@Service
public class AccountUserDetailsService implements UserDetailsService {

    private static final String NO_ACCOUNT_MESSAGE = "No account for the given login";

    private final AccountRepository accountRepository;
    private final AccountPrincipalService accountPrincipalService;
    private final LoginNormalizer loginNormalizer;

    public AccountUserDetailsService(final AccountRepository accountRepository,
                                     final AccountPrincipalService accountPrincipalService,
                                     final LoginNormalizer loginNormalizer) {
        this.accountRepository = accountRepository;
        this.accountPrincipalService = accountPrincipalService;
        this.loginNormalizer = loginNormalizer;
    }

    @Override
    public UserDetails loadUserByUsername(final String username) {
        return accountPrincipalService.toUserDetails(
                accountRepository.findByUsernameIgnoreCase(loginNormalizer.normalize(username))
                        .orElseThrow(() -> new UsernameNotFoundException(NO_ACCOUNT_MESSAGE)));
    }
}
