package com.kenez92.plateplan.plan.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import com.kenez92.plateplan.plan.db.SavedDish;
import com.kenez92.plateplan.plan.db.SavedDishRepository;
import com.kenez92.plateplan.plan.model.DayMeals;
import com.kenez92.plateplan.plan.model.DishOpinion;
import com.kenez92.plateplan.plan.model.Meal;
import com.kenez92.plateplan.plan.model.SavedDishView;
import com.kenez92.plateplan.plan.model.WeekPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DishMemoryTest {

    @Test
    void shouldStoreEachDishOnceAndKeepTheFirstSpellingAndPreparation() {
        final DishMemory memory = memory();
        final WeekPlan first = week(day("Rosół", "Gotuj rosół.", "Jogurt", "Jogurt", "Schab", "Usmaż.", "Zupa", "Gotuj."));
        final WeekPlan again = week(day(" rosół ", "Inaczej.", "Jogurt", "Inny jogurt.", "   ", "Pusto.", "Bigos", "Duś."));

        memory.remember("ala", first);
        memory.remember("ola", week(day("Bigos", "Duś.", "Kanapka", "Podaj.", "Ryba", "Piecz.", "Sałatka", "Wymieszaj.")));
        memory.remember("ala", again);

        final List<SavedDishView> actual = memory.listed("ala");
        final List<SavedDishView> expected = List.of(
                view(9, "Bigos", "Duś.", null),
                view(4, "Zupa", "Gotuj.", null),
                view(3, "Schab", "Usmaż.", null),
                view(2, "Jogurt", "Jogurt", null),
                view(1, "Rosół", "Gotuj rosół.", null));
        final List<SavedDishView> otherActual = memory.listed("ola");
        final List<SavedDishView> otherExpected = List.of(
                view(8, "Sałatka", "Wymieszaj.", null),
                view(7, "Ryba", "Piecz.", null),
                view(6, "Kanapka", "Podaj.", null),
                view(5, "Bigos", "Duś.", null));

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
        assertThat(otherActual).usingRecursiveComparison().isEqualTo(otherExpected);
    }

    @Test
    void shouldKeepAMarkWhenTheSameDishReturnsAndIgnoreAnotherLogin() {
        final DishMemory memory = memory();

        memory.remember("ala", week(day("Rosół", "Gotuj.", "Jogurt", "Podaj.", "Schab", "Usmaż.", "Zupa", "Gotuj.")));
        final long rosol = memory.listed("ala").stream()
                .filter((final SavedDishView dish) -> dish.name().equals("Rosół"))
                .mapToLong(SavedDishView::dishId)
                .findFirst()
                .orElseThrow();
        memory.rate("ola", rosol, "DISLIKE");
        memory.rate("ala", rosol, "MAYBE");
        memory.rate("ala", rosol, "LIKE");
        memory.remember("ala", week(day(" rosół ", "Inaczej.", "Jogurt", "Inny.", "Schab", "Inaczej.", "Zupa", "Inaczej.")));

        final List<String> liked = memory.likedNames("ala");
        final List<String> disliked = memory.dislikedNames("ala");
        final SavedDishView actual = memory.listed("ala").stream()
                .filter((final SavedDishView dish) -> dish.dishId() == rosol)
                .findFirst()
                .orElseThrow();
        final SavedDishView expected = view(rosol, "Rosół", "Gotuj.", DishOpinion.LIKE);

        assertThat(liked).usingRecursiveComparison().isEqualTo(List.of("Rosół"));
        assertThat(disliked).usingRecursiveComparison().isEqualTo(List.of());
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
        assertThat(memory.likedNames("ola")).usingRecursiveComparison().isEqualTo(List.of());
    }

    @Test
    void shouldDropTheOldestUnratedDishesAndKeepARatedOne() {
        final DishMemory memory = memory();

        memory.remember("ala", numbered(0));
        final long kept = memory.listed("ala").stream()
                .filter((final SavedDishView dish) -> dish.name().equals("Danie 0"))
                .mapToLong(SavedDishView::dishId)
                .findFirst()
                .orElseThrow();
        memory.rate("ala", kept, "DISLIKE");
        memory.remember("ala", numbered(28));
        memory.remember("ala", numbered(56));
        memory.remember("ala", numbered(84));

        final List<String> names = memory.listed("ala").stream().map(SavedDishView::name).toList();
        final List<String> disliked = memory.dislikedNames("ala");

        assertThat(names).hasSize(85);
        assertThat(names).contains("Danie 0", "Danie 28", "Danie 111");
        assertThat(names).doesNotContain("Danie 1", "Danie 27");
        assertThat(disliked).usingRecursiveComparison().isEqualTo(List.of("Danie 0"));
    }

    private DishMemory memory() {
        final List<SavedDish> rows = new ArrayList<>();
        final AtomicLong ids = new AtomicLong();
        final SavedDishRepository repository = mock(SavedDishRepository.class);
        when(repository.saveAll(any())).thenAnswer((final InvocationOnMock invocation) -> {
            final List<SavedDish> stored = new ArrayList<>();
            invocation.<Iterable<SavedDish>>getArgument(0).forEach((final SavedDish row) -> {
                final SavedDish saved = new SavedDish(ids.incrementAndGet(), row.getLogin(), row.getName(),
                        row.getPreparation(), row.getOpinion());
                rows.add(saved);
                stored.add(saved);
            });
            return stored;
        });
        lenient().when(repository.save(any(SavedDish.class))).thenAnswer((final InvocationOnMock invocation) ->
                invocation.getArgument(0));
        when(repository.findByLoginOrderByDishIdAsc(anyString())).thenAnswer((final InvocationOnMock invocation) ->
                rows.stream()
                        .filter((final SavedDish row) -> row.getLogin().equals(invocation.getArgument(0)))
                        .sorted(Comparator.comparing(SavedDish::getDishId))
                        .toList());
        when(repository.findByLoginOrderByDishIdDesc(anyString())).thenAnswer((final InvocationOnMock invocation) ->
                rows.stream()
                        .filter((final SavedDish row) -> row.getLogin().equals(invocation.getArgument(0)))
                        .sorted(Comparator.comparing(SavedDish::getDishId).reversed())
                        .toList());
        lenient().when(repository.findById(anyLong())).thenAnswer((final InvocationOnMock invocation) -> rows.stream()
                .filter((final SavedDish row) -> row.getDishId().equals(invocation.getArgument(0)))
                .findFirst());
        lenient().when(repository.deleteByDishIdInAndLogin(any(), anyString()))
                .thenAnswer((final InvocationOnMock invocation) -> {
                    final Collection<Long> deletedIds = invocation.getArgument(0);
                    final List<SavedDish> removed = rows.stream()
                            .filter((final SavedDish row) -> deletedIds.contains(row.getDishId())
                                    && row.getLogin().equals(invocation.getArgument(1)))
                            .toList();
                    rows.removeAll(removed);
                    return removed.size();
                });
        return new DishMemory(repository);
    }

    private SavedDishView view(final long dishId,
                               final String name,
                               final String preparation,
                               final DishOpinion opinion) {
        return new SavedDishView(dishId, name, preparation, opinion);
    }

    private WeekPlan week(final DayMeals day) {
        return new WeekPlan(List.of(day), List.of());
    }

    private WeekPlan numbered(final int from) {
        return new WeekPlan(IntStream.range(0, 7)
                .mapToObj((final int day) -> day(from + day * 4))
                .toList(), List.of());
    }

    private DayMeals day(final int from) {
        return new DayMeals(
                meal("Danie " + from, "s"),
                meal("Danie " + (from + 1), "d"),
                meal("Danie " + (from + 2), "o"),
                meal("Danie " + (from + 3), "k"));
    }

    private DayMeals day(final String breakfast,
                         final String breakfastPreparation,
                         final String secondBreakfast,
                         final String secondPreparation,
                         final String lunch,
                         final String lunchPreparation,
                         final String dinner,
                         final String dinnerPreparation) {
        return new DayMeals(
                meal(breakfast, breakfastPreparation),
                meal(secondBreakfast, secondPreparation),
                meal(lunch, lunchPreparation),
                meal(dinner, dinnerPreparation));
    }

    private Meal meal(final String name, final String preparation) {
        return new Meal(name, preparation, List.of());
    }
}
