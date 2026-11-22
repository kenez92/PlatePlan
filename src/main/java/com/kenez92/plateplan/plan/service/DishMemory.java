package com.kenez92.plateplan.plan.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.kenez92.plateplan.plan.db.SavedDish;
import com.kenez92.plateplan.plan.db.SavedDishRepository;
import com.kenez92.plateplan.plan.model.DayMeals;
import com.kenez92.plateplan.plan.model.DishOpinion;
import com.kenez92.plateplan.plan.model.Meal;
import com.kenez92.plateplan.plan.model.SavedDishView;
import com.kenez92.plateplan.plan.model.WeekPlan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores each dish of a successful week once for one login, with the preparation from the first
 * time that name was saved. A mark is {@link DishOpinion#LIKE} or {@link DishOpinion#DISLIKE} for
 * the whole account. Unrated dishes past three weeks of four meals are dropped, oldest first. A
 * marked dish stays. Another login is a different set of rows. Names, the preparation, the login,
 * and the week are not logged.
 */
@Service
public class DishMemory {

    private static final int UNRATED_KEPT = 84;

    private final SavedDishRepository savedDishRepository;

    public DishMemory(final SavedDishRepository savedDishRepository) {
        this.savedDishRepository = savedDishRepository;
    }

    /**
     * Dishes of {@code login}, newest first. No dishes is an empty list.
     */
    @Transactional(readOnly = true)
    public List<SavedDishView> listed(final String login) {
        return savedDishRepository.findByLoginOrderByDishIdDesc(login).stream()
                .map(this::view)
                .toList();
    }

    /**
     * Liked names, oldest mark first. The same name after strip and {@link Locale#ROOT} lower case
     * is kept once, in the first spelling. No marks is an empty list.
     */
    @Transactional(readOnly = true)
    public List<String> likedNames(final String login) {
        return names(login, DishOpinion.LIKE);
    }

    /**
     * Disliked names, oldest mark first. The same name after strip and {@link Locale#ROOT} lower
     * case is kept once, in the first spelling. No marks is an empty list.
     */
    @Transactional(readOnly = true)
    public List<String> dislikedNames(final String login) {
        return names(login, DishOpinion.DISLIKE);
    }

    /**
     * Stores each new dish of the week, then drops older unrated dishes of this login until
     * {@value #UNRATED_KEPT} remain. A name already stored keeps its preparation and its mark. The
     * insert and the deletes are one transaction.
     */
    @Transactional
    public void remember(final String login, final WeekPlan week) {
        final List<SavedDish> fresh = newcomers(login, week, byKey(rows(login)));
        if (!fresh.isEmpty()) {
            savedDishRepository.saveAll(fresh);
        }
        trim(login);
    }

    /**
     * Sets the mark when {@code opinion} is a mark and the dish belongs to {@code login}. Anything
     * else changes nothing.
     */
    @Transactional
    public void rate(final String login, final long dishId, final String opinion) {
        final DishOpinion mark = DishOpinion.fromToken(opinion);
        if (mark == null) {
            return;
        }
        savedDishRepository.findById(dishId)
                .filter((final SavedDish dish) -> login.equals(dish.getLogin()))
                .ifPresent((final SavedDish dish) -> mark(dish, mark));
    }

    private void mark(final SavedDish dish, final DishOpinion mark) {
        dish.setOpinion(mark);
        savedDishRepository.save(dish);
    }

    private List<String> names(final String login, final DishOpinion mark) {
        return rows(login).stream()
                .filter((final SavedDish dish) -> dish.getOpinion() == mark)
                .map(SavedDish::getName)
                .toList();
    }

    private List<SavedDish> rows(final String login) {
        return savedDishRepository.findByLoginOrderByDishIdAsc(login);
    }

    private void trim(final String login) {
        final List<SavedDish> unrated = rows(login).stream()
                .filter((final SavedDish dish) -> dish.getOpinion() == null)
                .toList();
        final List<Long> dishIds = unrated.stream()
                .limit(Math.max(0, unrated.size() - UNRATED_KEPT))
                .map(SavedDish::getDishId)
                .toList();
        if (dishIds.isEmpty()) {
            return;
        }
        savedDishRepository.deleteByDishIdInAndLogin(dishIds, login);
    }

    private List<SavedDish> newcomers(final String login,
                                      final WeekPlan week,
                                      final Map<String, SavedDish> stored) {
        return meals(week).stream()
                .filter((final Meal meal) -> !nameKey(meal.name()).isEmpty())
                .filter((final Meal meal) -> !stored.containsKey(nameKey(meal.name())))
                .collect(Collectors.toMap(
                        (final Meal meal) -> nameKey(meal.name()),
                        (final Meal meal) -> new SavedDish(login, meal.name(), meal.preparation()),
                        (final SavedDish first, final SavedDish ignored) -> first,
                        LinkedHashMap::new))
                .values()
                .stream()
                .toList();
    }

    private Map<String, SavedDish> byKey(final List<SavedDish> dishes) {
        return dishes.stream()
                .collect(Collectors.toMap(
                        (final SavedDish dish) -> nameKey(dish.getName()),
                        (final SavedDish dish) -> dish,
                        (final SavedDish first, final SavedDish ignored) -> first,
                        LinkedHashMap::new));
    }

    private List<Meal> meals(final WeekPlan week) {
        return week.days().stream()
                .flatMap((final DayMeals day) -> Stream.of(
                        day.breakfast(), day.secondBreakfast(), day.lunch(), day.dinner()))
                .toList();
    }

    private SavedDishView view(final SavedDish dish) {
        return new SavedDishView(dish.getDishId(), dish.getName(), dish.getPreparation(), dish.getOpinion());
    }

    private String nameKey(final String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }
}
