package com.kenez92.plateplan.account;

import java.util.List;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Lets Spring Security sign a person in against the account table. It queries only when a login
 * arrives, never at start, so the application still starts without a database. Accounts have no
 * roles, so the authority list is empty. Nothing about the account is logged here.
 */
@Service
public class AccountUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    public AccountUserDetailsService(final AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(final String username) {
        return createUserDetails(accountRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("No account for the given login")));
    }

    private UserDetails createUserDetails(final Account account) {
        return User.withUsername(account.getUsername())
                .password(account.getPasswordHash())
                .authorities(List.of())
                .build();
    }
}
