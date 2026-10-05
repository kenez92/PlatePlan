package com.kenez92.plateplan.profile.db;

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
}
