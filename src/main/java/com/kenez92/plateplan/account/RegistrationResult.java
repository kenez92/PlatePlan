package com.kenez92.plateplan.account;

/**
 * The outcome of a registration: the created account, or the one reason it was refused.
 */
public record RegistrationResult(Account account, RegistrationError error) {

    public static RegistrationResult created(final Account account) {
        return new RegistrationResult(account, null);
    }

    public static RegistrationResult rejected(final RegistrationError error) {
        return new RegistrationResult(null, error);
    }

    public boolean isCreated() {
        return account != null;
    }
}
