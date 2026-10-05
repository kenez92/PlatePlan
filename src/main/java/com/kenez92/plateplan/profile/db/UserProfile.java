package com.kenez92.plateplan.profile.db;

import java.math.BigDecimal;

import com.kenez92.plateplan.profile.model.ActivityLevel;
import com.kenez92.plateplan.profile.model.Goal;
import com.kenez92.plateplan.profile.model.Sex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The profile of one account. The login is the primary key, exactly as the account stores it. It
 * holds the stored values and no rule; the product columns are the stored text, names joined with a
 * semicolon, and null when there are none. The daily calorie target is null until the first save of
 * the body fields, which stores the formula. Later the client edits only that column. The row is
 * created by the first save.
 */
@Entity
@Table(name = "user_profile")
public class UserProfile {

    @Id
    @Column(name = "login", nullable = false)
    private String login;

    @Column(name = "age", nullable = false)
    private int age;

    @Column(name = "height_cm", nullable = false)
    private int heightCm;

    @Column(name = "weight_kg", nullable = false, precision = 4, scale = 1)
    private BigDecimal weightKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "sex", nullable = false)
    private Sex sex;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal", nullable = false)
    private Goal goal;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level", nullable = false)
    private ActivityLevel activityLevel;

    @Column(name = "confirmed_calories")
    private Integer confirmedCalories;

    @Column(name = "preferred_products")
    private String preferredProducts;

    @Column(name = "excluded_products")
    private String excludedProducts;

    protected UserProfile() {
    }

    public UserProfile(final String login,
                       final int age,
                       final int heightCm,
                       final BigDecimal weightKg,
                       final Sex sex,
                       final Goal goal,
                       final ActivityLevel activityLevel,
                       final Integer confirmedCalories,
                       final String preferredProducts,
                       final String excludedProducts) {
        this.login = login;
        replaceValues(age, heightCm, weightKg, sex, goal, activityLevel, confirmedCalories, preferredProducts,
                excludedProducts);
    }

    /**
     * Replaces every value but the login.
     */
    public void replaceValues(final int age,
                              final int heightCm,
                              final BigDecimal weightKg,
                              final Sex sex,
                              final Goal goal,
                              final ActivityLevel activityLevel,
                              final Integer confirmedCalories,
                              final String preferredProducts,
                              final String excludedProducts) {
        this.age = age;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.sex = sex;
        this.goal = goal;
        this.activityLevel = activityLevel;
        this.confirmedCalories = confirmedCalories;
        this.preferredProducts = preferredProducts;
        this.excludedProducts = excludedProducts;
    }

    /**
     * Replaces only the daily calorie target.
     */
    public void replaceConfirmedCalories(final Integer confirmedCalories) {
        this.confirmedCalories = confirmedCalories;
    }

    public String getLogin() {
        return login;
    }

    public int getAge() {
        return age;
    }

    public int getHeightCm() {
        return heightCm;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public Sex getSex() {
        return sex;
    }

    public Goal getGoal() {
        return goal;
    }

    public ActivityLevel getActivityLevel() {
        return activityLevel;
    }

    public Integer getConfirmedCalories() {
        return confirmedCalories;
    }

    public String getPreferredProducts() {
        return preferredProducts;
    }

    public String getExcludedProducts() {
        return excludedProducts;
    }
}
