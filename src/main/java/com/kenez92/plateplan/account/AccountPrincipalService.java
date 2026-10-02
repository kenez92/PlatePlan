package com.kenez92.plateplan.account;

import java.util.List;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Turns an account into the principal Spring Security works with. Sign-in by login and sign-in
 * right after registration both use it, so both create the same kind of principal. Accounts have
 * no roles, so the authority list is empty.
 */
@Service
public class AccountPrincipalService {

    public UserDetails toUserDetails(final Account account) {
        return User.withUsername(account.getUsername())
                .password(account.getPasswordHash())
                .authorities(List.of())
                .build();
    }
}
