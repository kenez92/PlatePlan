package com.kenez92.plateplan.plan.model;

/**
 * The account's mark on a saved dish. One mark covers the whole account. {@code null} on the row
 * means the dish is saved and not marked yet.
 */
public enum DishOpinion {

    LIKE,
    DISLIKE;

    private static final String LIKE_TOKEN = "LIKE";
    private static final String DISLIKE_TOKEN = "DISLIKE";

    /**
     * The form value, or {@code null} when the token is not a mark.
     */
    public static DishOpinion fromToken(final String token) {
        if (LIKE_TOKEN.equals(token)) {
            return LIKE;
        }
        if (DISLIKE_TOKEN.equals(token)) {
            return DISLIKE;
        }
        return null;
    }
}
