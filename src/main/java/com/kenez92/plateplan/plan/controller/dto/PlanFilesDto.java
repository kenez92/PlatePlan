package com.kenez92.plateplan.plan.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The JSON body of a generate response: two PDFs as {@code byte[]} (Jackson writes Base64), or a
 * single {@code error}. Success never includes {@code error}; failure never includes PDF fields.
 * {@code emailError} is only on a successful PDF pair when a requested send did not complete.
 * {@code dayCount} is how many days are in those files. {@link #toString()} omits the bytes so
 * they are not dumped in logs.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PlanFilesDto(byte[] dietPdf, byte[] shoppingListPdf, String error, String emailError,
                           Integer dayCount) {

    private static final String ERROR_UNAVAILABLE = "UNAVAILABLE";
    private static final String ERROR_PROFILE_REQUIRED = "PROFILE_REQUIRED";
    private static final String ERROR_CALORIES_REQUIRED = "CALORIES_REQUIRED";
    private static final String ERROR_SELECTION_REQUIRED = "SELECTION_REQUIRED";
    private static final String SUCCESS_STRING =
            "PlanFilesDto[dietPdf=<omitted>, shoppingListPdf=<omitted>, dayCount=%d]";
    private static final String ERROR_STRING = "PlanFilesDto[error=%s]";
    private static final String EMAIL_ERROR_STRING =
            "PlanFilesDto[dietPdf=<omitted>, shoppingListPdf=<omitted>, emailError=%s, dayCount=%d]";

    private static final String INCOMPLETE_SUCCESS = "Success requires both PDF fields, a day count, and no error";
    private static final int LEAST_DAY = 1;

    public PlanFilesDto {
        if (error != null) {
            dietPdf = null;
            shoppingListPdf = null;
            emailError = null;
            dayCount = null;
        } else if (dietPdf == null || shoppingListPdf == null || dayCount == null || dayCount < LEAST_DAY) {
            throw new IllegalArgumentException(INCOMPLETE_SUCCESS);
        }
    }

    public static PlanFilesDto success(final byte[] dietPdf, final byte[] shoppingListPdf, final int dayCount) {
        return new PlanFilesDto(dietPdf, shoppingListPdf, null, null, dayCount);
    }

    public static PlanFilesDto successWithEmailUnavailable(final byte[] dietPdf,
                                                           final byte[] shoppingListPdf,
                                                           final int dayCount) {
        return new PlanFilesDto(dietPdf, shoppingListPdf, null, ERROR_UNAVAILABLE, dayCount);
    }

    public static PlanFilesDto unavailable() {
        return new PlanFilesDto(null, null, ERROR_UNAVAILABLE, null, null);
    }

    public static PlanFilesDto profileRequired() {
        return new PlanFilesDto(null, null, ERROR_PROFILE_REQUIRED, null, null);
    }

    public static PlanFilesDto caloriesRequired() {
        return new PlanFilesDto(null, null, ERROR_CALORIES_REQUIRED, null, null);
    }

    public static PlanFilesDto selectionRequired() {
        return new PlanFilesDto(null, null, ERROR_SELECTION_REQUIRED, null, null);
    }

    @Override
    public String toString() {
        if (error != null) {
            return ERROR_STRING.formatted(error);
        }
        if (emailError != null) {
            return EMAIL_ERROR_STRING.formatted(emailError, dayCount);
        }
        return SUCCESS_STRING.formatted(dayCount);
    }
}
