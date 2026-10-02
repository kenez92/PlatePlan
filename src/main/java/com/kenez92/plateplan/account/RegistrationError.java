package com.kenez92.plateplan.account;

/**
 * Why a registration did not create an account. Each value maps to one message on the form.
 */
public enum RegistrationError {
    LOGIN_INVALID,
    PASSWORD_INVALID,
    LOGIN_TAKEN,
    UNAVAILABLE
}
