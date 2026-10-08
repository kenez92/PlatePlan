package com.kenez92.plateplan.profile.db;

import java.math.BigDecimal;

import com.kenez92.plateplan.profile.model.ActivityLevel;
import com.kenez92.plateplan.profile.model.Goal;
import com.kenez92.plateplan.profile.model.Sex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {

    /**
     * Writes only {@code confirmed_calories}. Returns the number of matching rows.
     */
    @Modifying
    @Transactional
    @Query("update UserProfile profile set profile.confirmedCalories = :confirmedCalories where profile.login = :login")
    int replaceConfirmedCalories(@Param("login") final String login,
                                 @Param("confirmedCalories") final int confirmedCalories);

    /**
     * Writes every column except {@code confirmed_calories}. Returns the number of matching rows.
     */
    @Modifying
    @Transactional
    @Query("update UserProfile profile set profile.age = :age, profile.heightCm = :heightCm, "
            + "profile.weightKg = :weightKg, profile.sex = :sex, profile.goal = :goal, "
            + "profile.activityLevel = :activityLevel, profile.preferredProducts = :preferredProducts, "
            + "profile.excludedProducts = :excludedProducts where profile.login = :login")
    int replaceBodyAndProducts(@Param("login") final String login,
                               @Param("age") final int age,
                               @Param("heightCm") final int heightCm,
                               @Param("weightKg") final BigDecimal weightKg,
                               @Param("sex") final Sex sex,
                               @Param("goal") final Goal goal,
                               @Param("activityLevel") final ActivityLevel activityLevel,
                               @Param("preferredProducts") final String preferredProducts,
                               @Param("excludedProducts") final String excludedProducts);

    /**
     * Writes body, products, and {@code confirmed_calories} in one statement. Used when the first
     * body save of an existing row also stores the formula.
     */
    @Modifying
    @Transactional
    @Query("update UserProfile profile set profile.age = :age, profile.heightCm = :heightCm, "
            + "profile.weightKg = :weightKg, profile.sex = :sex, profile.goal = :goal, "
            + "profile.activityLevel = :activityLevel, profile.confirmedCalories = :confirmedCalories, "
            + "profile.preferredProducts = :preferredProducts, "
            + "profile.excludedProducts = :excludedProducts where profile.login = :login")
    int replaceBodyProductsAndCalories(@Param("login") final String login,
                                       @Param("age") final int age,
                                       @Param("heightCm") final int heightCm,
                                       @Param("weightKg") final BigDecimal weightKg,
                                       @Param("sex") final Sex sex,
                                       @Param("goal") final Goal goal,
                                       @Param("activityLevel") final ActivityLevel activityLevel,
                                       @Param("confirmedCalories") final int confirmedCalories,
                                       @Param("preferredProducts") final String preferredProducts,
                                       @Param("excludedProducts") final String excludedProducts);
}
