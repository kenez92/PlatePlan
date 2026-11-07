package com.kenez92.plateplan.plan.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One lunch name from a remembered week. Breakfast, second breakfast, and dinner are not stored.
 * Deleting the plan removes the name through the foreign key.
 */
@Entity
@Table(name = "recent_lunch_name")
public class RecentLunchName {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lunch_name_id", nullable = false)
    private Long lunchNameId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "name", nullable = false)
    private String name;

    protected RecentLunchName() {
    }

    public RecentLunchName(final Long planId, final String name) {
        this(null, planId, name);
    }

    public RecentLunchName(final Long lunchNameId, final Long planId, final String name) {
        this.lunchNameId = lunchNameId;
        this.planId = planId;
        this.name = name;
    }

    public Long getLunchNameId() {
        return lunchNameId;
    }

    public Long getPlanId() {
        return planId;
    }

    public String getName() {
        return name;
    }
}
