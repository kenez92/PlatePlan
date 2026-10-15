package com.kenez92.plateplan.profile.db;

import java.math.BigDecimal;
import java.util.List;

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
public interface HouseholdMemberRepository extends JpaRepository<HouseholdMember, Long> {

    /**
     * The added people of one account, oldest id first.
     */
    List<HouseholdMember> findByLoginOrderByMemberIdAsc(final String login);

    /**
     * Writes the name, body, and product lists of one row of this login. The calorie target stays.
     * Returns the number of matching rows. An id of another account matches nothing.
     */
    @Modifying
    @Transactional
    @Query("update HouseholdMember member set member.displayName = :displayName, member.age = :age, "
            + "member.heightCm = :heightCm, member.weightKg = :weightKg, member.sex = :sex, member.goal = :goal, "
            + "member.activityLevel = :activityLevel, member.preferredProducts = :preferredProducts, "
            + "member.excludedProducts = :excludedProducts "
            + "where member.memberId = :memberId and member.login = :login")
    int replaceBody(@Param("memberId") final long memberId,
                    @Param("login") final String login,
                    @Param("displayName") final String displayName,
                    @Param("age") final int age,
                    @Param("heightCm") final int heightCm,
                    @Param("weightKg") final BigDecimal weightKg,
                    @Param("sex") final Sex sex,
                    @Param("goal") final Goal goal,
                    @Param("activityLevel") final ActivityLevel activityLevel,
                    @Param("preferredProducts") final String preferredProducts,
                    @Param("excludedProducts") final String excludedProducts);

    /**
     * Removes one row of this login. Returns the number of matching rows. An id of another account
     * matches nothing.
     */
    @Modifying
    @Transactional
    @Query("delete from HouseholdMember member where member.memberId = :memberId and member.login = :login")
    int deleteByMemberIdAndLogin(@Param("memberId") final long memberId, @Param("login") final String login);
}
