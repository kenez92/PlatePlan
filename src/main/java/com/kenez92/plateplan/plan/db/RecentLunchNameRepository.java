package com.kenez92.plateplan.plan.db;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Lunch names of remembered weeks. A read takes plan ids. Inserts use {@code saveAll}.
 */
@Repository
public interface RecentLunchNameRepository extends JpaRepository<RecentLunchName, Long> {

    /**
     * Lunch names of the given plans, {@code plan_id} ascending and then {@code lunch_name_id}
     * ascending, which is day order when the names were inserted in day order.
     */
    List<RecentLunchName> findByPlanIdInOrderByPlanIdAscLunchNameIdAsc(final Collection<Long> planIds);
}
