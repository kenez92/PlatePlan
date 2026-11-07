package com.kenez92.plateplan.plan.db;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface RecentPlanRepository extends JpaRepository<RecentPlan, Long> {

    /**
     * Remembered weeks of one login, oldest {@code plan_id} first.
     */
    List<RecentPlan> findByLoginOrderByPlanIdAsc(final String login);

    /**
     * Removes one week of this login. Returns the number of matching rows. A plan of another login
     * matches nothing.
     */
    @Modifying
    @Transactional
    @Query("delete from RecentPlan plan where plan.planId = :planId and plan.login = :login")
    int deleteByPlanIdAndLogin(@Param("planId") final long planId, @Param("login") final String login);
}
