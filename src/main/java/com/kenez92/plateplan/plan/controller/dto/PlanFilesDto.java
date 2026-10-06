package com.kenez92.plateplan.plan.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The JSON body of a generate response: two PDFs as {@code byte[]} (Jackson writes Base64), or a
 * single {@code error}. Success never includes {@code error}; failure never includes PDF fields.
 * {@link #toString()} omits the bytes so they are not dumped in logs.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PlanFilesDto(byte[] dietPdf, byte[] shoppingListPdf, String error) {

    private static final String ERROR_UNAVAILABLE = "UNAVAILABLE";
    private static final String ERROR_PROFILE_REQUIRED = "PROFILE_REQUIRED";
    private static final String ERROR_CALORIES_REQUIRED = "CALORIES_REQUIRED";
    private static final String SUCCESS_STRING = "PlanFilesDto[dietPdf=<omitted>, shoppingListPdf=<omitted>]";
    private static final String ERROR_STRING = "PlanFilesDto[error=%s]";

    public PlanFilesDto {
        if (error != null) {
            dietPdf = null;
            shoppingListPdf = null;
        }
    }

    public static PlanFilesDto success(final byte[] dietPdf, final byte[] shoppingListPdf) {
        return new PlanFilesDto(dietPdf, shoppingListPdf, null);
    }

    public static PlanFilesDto unavailable() {
        return new PlanFilesDto(null, null, ERROR_UNAVAILABLE);
    }

    public static PlanFilesDto profileRequired() {
        return new PlanFilesDto(null, null, ERROR_PROFILE_REQUIRED);
    }

    public static PlanFilesDto caloriesRequired() {
        return new PlanFilesDto(null, null, ERROR_CALORIES_REQUIRED);
    }

    @Override
    public String toString() {
        if (error != null) {
            return ERROR_STRING.formatted(error);
        }
        return SUCCESS_STRING;
    }
}
