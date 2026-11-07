package com.kenez92.plateplan.plan.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One remembered week of an account. The login is stored as the account's login and is not a
 * foreign key. {@code plan_id} is an identity primary key; an older week has a smaller id.
 */
@Entity
@Table(name = "recent_plan")
public class RecentPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "login", nullable = false)
    private String login;

    protected RecentPlan() {
    }

    public RecentPlan(final String login) {
        this(null, login);
    }

    public RecentPlan(final Long planId, final String login) {
        this.planId = planId;
        this.login = login;
    }

    public Long getPlanId() {
        return planId;
    }

    public String getLogin() {
        return login;
    }
}
