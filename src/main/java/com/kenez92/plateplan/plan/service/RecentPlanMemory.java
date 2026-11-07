package com.kenez92.plateplan.plan.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.kenez92.plateplan.plan.db.RecentLunchName;
import com.kenez92.plateplan.plan.db.RecentLunchNameRepository;
import com.kenez92.plateplan.plan.db.RecentPlan;
import com.kenez92.plateplan.plan.db.RecentPlanRepository;
import com.kenez92.plateplan.plan.model.DayMeals;
import com.kenez92.plateplan.plan.model.WeekPlan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remembers the lunch names of a successful week and returns lunches from at most the three newest
 * plans of one login. Breakfast, second breakfast, and dinner are not stored. Another login is a
 * different set of rows. Names, the login, and the week are not logged.
 */
@Service
public class RecentPlanMemory {

    private static final int KEPT_PLANS = 3;

    private final RecentPlanRepository recentPlanRepository;
    private final RecentLunchNameRepository recentLunchNameRepository;

    public RecentPlanMemory(final RecentPlanRepository recentPlanRepository,
                            final RecentLunchNameRepository recentLunchNameRepository) {
        this.recentPlanRepository = recentPlanRepository;
        this.recentLunchNameRepository = recentLunchNameRepository;
    }

    /**
     * Lunch names from the three newest plans of {@code login}, oldest of those three first, then
     * day order. The same name after strip and {@link Locale#ROOT} lower case is kept once, in the
     * first spelling. No plans is an empty list.
     */
    public List<String> avoidedNames(final String login) {
        final List<RecentPlan> plans = newest(recentPlanRepository.findByLoginOrderByPlanIdAsc(login));
        if (plans.isEmpty()) {
            return List.of();
        }
        return distinct(lunchNames(plans));
    }

    /**
     * Stores one plan and the lunch of each day, then drops older plans of this login until three
     * remain, smallest {@code plan_id} first. The insert and the deletes are one transaction.
     */
    @Transactional
    public void remember(final String login, final WeekPlan week) {
        recentLunchNameRepository.saveAll(
                lunches(recentPlanRepository.save(new RecentPlan(login)).getPlanId(), week));
        trim(login);
    }

    private void trim(final String login) {
        excess(recentPlanRepository.findByLoginOrderByPlanIdAsc(login)).forEach((final RecentPlan plan) ->
                recentPlanRepository.deleteByPlanIdAndLogin(plan.getPlanId(), login));
    }

    private List<RecentLunchName> lunches(final Long planId, final WeekPlan week) {
        return week.days().stream()
                .map((final DayMeals day) -> new RecentLunchName(planId, day.lunch().name()))
                .toList();
    }

    private List<String> lunchNames(final List<RecentPlan> plans) {
        return recentLunchNameRepository
                .findByPlanIdInOrderByPlanIdAscLunchNameIdAsc(plans.stream().map(RecentPlan::getPlanId).toList())
                .stream()
                .map(RecentLunchName::getName)
                .toList();
    }

    private List<RecentPlan> newest(final List<RecentPlan> plans) {
        return plans.stream().skip(Math.max(0, plans.size() - KEPT_PLANS)).toList();
    }

    private List<RecentPlan> excess(final List<RecentPlan> plans) {
        return plans.stream().limit(Math.max(0, plans.size() - KEPT_PLANS)).toList();
    }

    private List<String> distinct(final List<String> names) {
        return names.stream()
                .collect(Collectors.toMap(
                        this::nameKey,
                        (final String name) -> name,
                        (final String first, final String ignored) -> first,
                        LinkedHashMap::new))
                .values()
                .stream()
                .toList();
    }

    private String nameKey(final String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }
}
