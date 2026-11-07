package com.kenez92.plateplan.plan.db;

import java.util.Collection;
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
     * Removes these weeks of this login. Returns the number of matching rows. A plan of another
     * login matches nothing. The insert of the new week is flushed first, and the persistence
     * context is cleared so a deleted row is not written again on commit.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("delete from RecentPlan plan where plan.planId in :planIds and plan.login = :login")
    int deleteByPlanIdInAndLogin(@Param("planIds") final Collection<Long> planIds,
                                 @Param("login") final String login);
}
