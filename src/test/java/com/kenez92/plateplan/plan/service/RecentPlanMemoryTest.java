package com.kenez92.plateplan.plan.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.kenez92.plateplan.plan.db.RecentLunchName;
import com.kenez92.plateplan.plan.db.RecentLunchNameRepository;
import com.kenez92.plateplan.plan.db.RecentPlan;
import com.kenez92.plateplan.plan.db.RecentPlanRepository;
import com.kenez92.plateplan.plan.model.DayMeals;
import com.kenez92.plateplan.plan.model.Meal;
import com.kenez92.plateplan.plan.model.WeekPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecentPlanMemoryTest {

    @Test
    void shouldKeepLunchesFromTheThreeNewestPlansOfThatLogin() {
        final RecentPlanMemory memory = memory();
        final List<String> empty = memory.avoidedNames("ala");

        memory.remember("ola", week("Obiad B"));
        memory.remember("ala", week("Obiad 1"));
        memory.remember("ala", week("Obiad 2"));
        memory.remember("ala", week("Obiad 3"));
        memory.remember("ala", week("Obiad 4"));

        final List<String> actual = memory.avoidedNames("ala");
        final List<String> expected = Stream.of(lunches("Obiad 2"), lunches("Obiad 3"), lunches("Obiad 4"))
                .flatMap(List::stream)
                .toList();
        final List<String> otherActual = memory.avoidedNames("ola");
        final List<String> otherExpected = lunches("Obiad B");

        assertThat(empty).usingRecursiveComparison().isEqualTo(List.of());
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
        assertThat(otherActual).usingRecursiveComparison().isEqualTo(otherExpected);
    }

    @Test
    void shouldKeepTheFirstSpellingOfARepeatedLunch() {
        final RecentPlanMemory memory = memory();

        memory.remember("ala", weekWithLunches(List.of(
                "Schab", "Rosół", "Zupa", "Bigos", "Ryba", "Kurczak", "Gulasz")));
        memory.remember("ala", weekWithLunches(List.of(
                " schab ", "Rosół", "Zapiekanka", "Bigos", "Ryba", "Kurczak", "Gulasz")));

        final List<String> actual = memory.avoidedNames("ala");
        final List<String> expected = List.of(
                "Schab", "Rosół", "Zupa", "Bigos", "Ryba", "Kurczak", "Gulasz", "Zapiekanka");

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    private RecentPlanMemory memory() {
        final List<RecentPlan> plans = new ArrayList<>();
        final List<RecentLunchName> names = new ArrayList<>();
        final AtomicLong planIds = new AtomicLong();
        final AtomicLong nameIds = new AtomicLong();
        final RecentPlanRepository planRepository = mock(RecentPlanRepository.class);
        final RecentLunchNameRepository nameRepository = mock(RecentLunchNameRepository.class);
        when(planRepository.save(any(RecentPlan.class))).thenAnswer((final InvocationOnMock invocation) -> {
            final RecentPlan stored = new RecentPlan(planIds.incrementAndGet(),
                    invocation.<RecentPlan>getArgument(0).getLogin());
            plans.add(stored);
            return stored;
        });
        when(planRepository.findByLoginOrderByPlanIdAsc(anyString())).thenAnswer((final InvocationOnMock invocation) ->
                plans.stream()
                        .filter((final RecentPlan plan) -> plan.getLogin().equals(invocation.getArgument(0)))
                        .sorted(Comparator.comparing(RecentPlan::getPlanId))
                        .toList());
        lenient().when(planRepository.deleteByPlanIdInAndLogin(any(), anyString()))
                .thenAnswer((final InvocationOnMock invocation) -> {
                    final Collection<Long> deletedIds = invocation.getArgument(0);
                    final List<RecentPlan> removed = plans.stream()
                            .filter((final RecentPlan plan) -> deletedIds.contains(plan.getPlanId())
                                    && plan.getLogin().equals(invocation.getArgument(1)))
                            .toList();
                    plans.removeAll(removed);
                    names.removeIf((final RecentLunchName row) -> removed.stream()
                            .anyMatch((final RecentPlan plan) -> plan.getPlanId().equals(row.getPlanId())));
                    return removed.size();
                });
        when(nameRepository.saveAll(any())).thenAnswer((final InvocationOnMock invocation) -> {
            final List<RecentLunchName> stored = StreamSupport.stream(
                            invocation.<Iterable<RecentLunchName>>getArgument(0).spliterator(), false)
                    .map((final RecentLunchName row) ->
                            new RecentLunchName(nameIds.incrementAndGet(), row.getPlanId(), row.getName()))
                    .toList();
            names.addAll(stored);
            return stored;
        });
        when(nameRepository.findByPlanIdInOrderByPlanIdAscLunchNameIdAsc(any()))
                .thenAnswer((final InvocationOnMock invocation) -> names.stream()
                        .filter((final RecentLunchName row) ->
                                invocation.<Collection<Long>>getArgument(0).contains(row.getPlanId()))
                        .sorted(Comparator.comparing(RecentLunchName::getPlanId)
                                .thenComparing(RecentLunchName::getLunchNameId))
                        .toList());
        return new RecentPlanMemory(planRepository, nameRepository);
    }

    private WeekPlan week(final String lunchPrefix) {
        return new WeekPlan(IntStream.rangeClosed(1, 7)
                .mapToObj((final int day) -> new DayMeals(
                        new Meal("Jajecznica", "", List.of()),
                        new Meal("Jogurt", "", List.of()),
                        new Meal(lunchPrefix + " " + day, "", List.of()),
                        new Meal("Kanapki", "", List.of())))
                .toList(), List.of());
    }

    private WeekPlan weekWithLunches(final List<String> lunches) {
        return new WeekPlan(lunches.stream()
                .map((final String lunch) -> new DayMeals(
                        new Meal("Jajecznica", "", List.of()),
                        new Meal("Jogurt", "", List.of()),
                        new Meal(lunch, "", List.of()),
                        new Meal("Kanapki", "", List.of())))
                .toList(), List.of());
    }

    private List<String> lunches(final String lunchPrefix) {
        return IntStream.rangeClosed(1, 7)
                .mapToObj((final int day) -> lunchPrefix + " " + day)
                .toList();
    }
}
