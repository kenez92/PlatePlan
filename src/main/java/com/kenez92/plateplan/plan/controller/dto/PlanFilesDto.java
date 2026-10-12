package com.kenez92.plateplan.plan.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The JSON body of a generate response: two PDFs as {@code byte[]} (Jackson writes Base64), or a
 * single {@code error}. Success never includes {@code error}; failure never includes PDF fields.
 * {@code emailError} is only on a successful PDF pair when a requested send did not complete.
 * {@link #toString()} omits the bytes so they are not dumped in logs.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PlanFilesDto(byte[] dietPdf, byte[] shoppingListPdf, String error, String emailError) {

    private static final String ERROR_UNAVAILABLE = "UNAVAILABLE";
    private static final String ERROR_PROFILE_REQUIRED = "PROFILE_REQUIRED";
    private static final String ERROR_CALORIES_REQUIRED = "CALORIES_REQUIRED";
    private static final String SUCCESS_STRING = "PlanFilesDto[dietPdf=<omitted>, shoppingListPdf=<omitted>]";
    private static final String ERROR_STRING = "PlanFilesDto[error=%s]";
    private static final String EMAIL_ERROR_STRING =
            "PlanFilesDto[dietPdf=<omitted>, shoppingListPdf=<omitted>, emailError=%s]";

    private static final String INCOMPLETE_SUCCESS = "Success requires both PDF fields and no error";

    public PlanFilesDto {
        if (error != null) {
            dietPdf = null;
            shoppingListPdf = null;
            emailError = null;
        } else if (dietPdf == null || shoppingListPdf == null) {
            throw new IllegalArgumentException(INCOMPLETE_SUCCESS);
        }
    }

    public static PlanFilesDto success(final byte[] dietPdf, final byte[] shoppingListPdf) {
        return new PlanFilesDto(dietPdf, shoppingListPdf, null, null);
    }

    public static PlanFilesDto successWithEmailUnavailable(final byte[] dietPdf, final byte[] shoppingListPdf) {
        return new PlanFilesDto(dietPdf, shoppingListPdf, null, ERROR_UNAVAILABLE);
    }

    public static PlanFilesDto unavailable() {
        return new PlanFilesDto(null, null, ERROR_UNAVAILABLE, null);
    }

    public static PlanFilesDto profileRequired() {
        return new PlanFilesDto(null, null, ERROR_PROFILE_REQUIRED, null);
    }

    public static PlanFilesDto caloriesRequired() {
        return new PlanFilesDto(null, null, ERROR_CALORIES_REQUIRED, null);
    }

    @Override
    public String toString() {
        if (error != null) {
            return ERROR_STRING.formatted(error);
        }
        if (emailError != null) {
            return EMAIL_ERROR_STRING.formatted(emailError);
        }
        return SUCCESS_STRING;
    }
}
