package com.kenez92.plateplan.account.service;

import java.util.List;

import com.kenez92.plateplan.account.db.Account;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

class AccountPrincipalServiceTest {

    @Test
    void shouldMapTheAccountToAPrincipalWithoutAuthorities() {
        final AccountPrincipalService service = new AccountPrincipalService();

        final UserDetails actual = service.toUserDetails(new Account("alice", "stored-hash"));

        final UserDetails expected = User.withUsername("alice")
                .password("stored-hash")
                .authorities(List.of())
                .build();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
}
