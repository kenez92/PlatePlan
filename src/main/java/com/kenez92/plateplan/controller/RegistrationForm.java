package com.kenez92.plateplan.controller;

/**
 * The fields of the registration form. A field the browser did not send is {@code null}; the
 * registration service treats that as invalid. The password and the login are private, so
 * {@link #toString()} shows neither.
 */
public record RegistrationForm(String username, String password) {

    private static final String REDACTED = "RegistrationForm[redacted]";

    @Override
    public String toString() {
        return REDACTED;
    }
}
