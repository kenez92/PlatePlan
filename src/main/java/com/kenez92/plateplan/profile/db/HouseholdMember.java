package com.kenez92.plateplan.profile.db;

import java.math.BigDecimal;

import com.kenez92.plateplan.profile.model.ActivityLevel;
import com.kenez92.plateplan.profile.model.Goal;
import com.kenez92.plateplan.profile.model.Sex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One added person on an account. The login is the account's login and is not a foreign key. The
 * display name is required. The row holds the same body, goal, activity, calorie target, and
 * product lists as the owner, and no email. The product columns are the stored text, names joined
 * with a semicolon, and null when there are none. The daily calorie target is set on the first
 * save from the formula and is left unchanged by a later body save.
 */
@Entity
@Table(name = "household_member")
public class HouseholdMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "login", nullable = false)
    private String login;

    @Column(name = "display_name", nullable = false)
    private String displayName;

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

    protected HouseholdMember() {
    }

    public HouseholdMember(final Long memberId,
                           final String login,
                           final String displayName,
                           final int age,
                           final int heightCm,
                           final BigDecimal weightKg,
                           final Sex sex,
                           final Goal goal,
                           final ActivityLevel activityLevel,
                           final Integer confirmedCalories,
                           final String preferredProducts,
                           final String excludedProducts) {
        this.memberId = memberId;
        this.login = login;
        this.confirmedCalories = confirmedCalories;
        replaceBody(displayName, age, heightCm, weightKg, sex, goal, activityLevel, preferredProducts,
                excludedProducts);
    }

    public HouseholdMember(final String login,
                           final String displayName,
                           final int age,
                           final int heightCm,
                           final BigDecimal weightKg,
                           final Sex sex,
                           final Goal goal,
                           final ActivityLevel activityLevel,
                           final Integer confirmedCalories,
                           final String preferredProducts,
                           final String excludedProducts) {
        this(null, login, displayName, age, heightCm, weightKg, sex, goal, activityLevel, confirmedCalories,
                preferredProducts, excludedProducts);
    }

    /**
     * Replaces the name, body, and product lists. The login and the calorie target stay.
     */
    public void replaceBody(final String displayName,
                            final int age,
                            final int heightCm,
                            final BigDecimal weightKg,
                            final Sex sex,
                            final Goal goal,
                            final ActivityLevel activityLevel,
                            final String preferredProducts,
                            final String excludedProducts) {
        this.displayName = displayName;
        this.age = age;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.sex = sex;
        this.goal = goal;
        this.activityLevel = activityLevel;
        this.preferredProducts = preferredProducts;
        this.excludedProducts = excludedProducts;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getLogin() {
        return login;
    }

    public String getDisplayName() {
        return displayName;
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
