package com.kenez92.plateplan.account;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Lets Spring Security sign a person in against the account table. It queries only when a login
 * arrives, never at start, so the application still starts without a database. Nothing about the
 * account is logged here.
 */
@Service
public class AccountUserDetailsService implements UserDetailsService {

    private static final String NO_ACCOUNT_MESSAGE = "No account for the given login";

    private final AccountRepository accountRepository;
    private final AccountPrincipalService accountPrincipalService;

    public AccountUserDetailsService(final AccountRepository accountRepository,
                                     final AccountPrincipalService accountPrincipalService) {
        this.accountRepository = accountRepository;
        this.accountPrincipalService = accountPrincipalService;
    }

    @Override
    public UserDetails loadUserByUsername(final String username) {
        return accountPrincipalService.toUserDetails(accountRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException(NO_ACCOUNT_MESSAGE)));
    }
}
