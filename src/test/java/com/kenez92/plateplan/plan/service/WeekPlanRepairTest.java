package com.kenez92.plateplan.plan.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import com.kenez92.plateplan.plan.model.DayMeals;
import com.kenez92.plateplan.plan.model.Ingredient;
import com.kenez92.plateplan.plan.model.Meal;
import com.kenez92.plateplan.plan.model.PlanPerson;
import com.kenez92.plateplan.plan.model.Portion;
import com.kenez92.plateplan.plan.model.ShoppingItem;
import com.kenez92.plateplan.plan.model.WeekPlan;
import com.kenez92.plateplan.plan.validator.WeekPlanValidator;
import com.kenez92.plateplan.profile.model.Goal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WeekPlanRepairTest {

    @Test
    void shouldScalePortionsOntoEachPersonsTargetAndUseTheAccountNames() {
        final List<PlanPerson> people = List.of(
                new PlanPerson("Ja", Goal.MAINTAIN, 2000),
                new PlanPerson("Anna", Goal.LOSE_WEIGHT, 800));
        final WeekPlanRepair repair = new WeekPlanRepair(new WeekPlanValidator());

        final WeekPlan actual = repair.repair(week(portion("X", 100), portion("Y", 100)), people);

        final WeekPlan expected = new WeekPlan(
                IntStream.range(0, 7).mapToObj(this::scaledDay).toList(),
                List.of(new ShoppingItem("jajka", "112 szt.")));
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldKeepAWeekThatAlreadyMatchesThePeopleAndTheTargets() {
        final List<PlanPerson> people = List.of(new PlanPerson("Ja", Goal.MAINTAIN, 2000));
        final WeekPlan week = new WeekPlan(IntStream.range(0, 7)
                .mapToObj((final int index) -> new DayMeals(
                        new Meal("Sniadanie " + index, "Kroki.", List.of(new Portion("Ja", List.of(), 2000, 10, 20, 5))),
                        new Meal("Jogurt", "Kroki.", List.of(new Portion("Ja", List.of(), 0, 0, 0, 0))),
                        new Meal("Obiad " + index, "Kroki.", List.of(new Portion("Ja", List.of(), 0, 0, 0, 0))),
                        new Meal("Kolacja " + index, "Kroki.", List.of(new Portion("Ja", List.of(), 0, 0, 0, 0)))))
                .toList(), List.of(new ShoppingItem("jogurt", "7 szt.")));
        final WeekPlanRepair repair = new WeekPlanRepair(new WeekPlanValidator());

        final WeekPlan actual = repair.repair(week, people);

        final WeekPlan expected = new WeekPlan(IntStream.range(0, 7)
                .mapToObj((final int index) -> new DayMeals(
                        new Meal("Sniadanie " + index, "Kroki.", List.of(new Portion("Ja", List.of(), 2000, 10, 479, 5))),
                        new Meal("Jogurt", "Kroki.", List.of(new Portion("Ja", List.of(), 0, 0, 0, 0))),
                        new Meal("Obiad " + index, "Kroki.", List.of(new Portion("Ja", List.of(), 0, 0, 0, 0))),
                        new Meal("Kolacja " + index, "Kroki.", List.of(new Portion("Ja", List.of(), 0, 0, 0, 0)))))
                .toList(), List.of());
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldSumEveryIngredientAcrossPeopleAndDaysAndDropAProductTheMealsDoNotUse() {
        final List<PlanPerson> people = List.of(
                new PlanPerson("Ja", Goal.MAINTAIN, 2000),
                new PlanPerson("Anna", Goal.LOSE_WEIGHT, 800));
        final Portion ja = new Portion("Ja", List.of(
                new Ingredient("jajka", "2 szt."),
                new Ingredient("masło", "10 g"),
                new Ingredient("sól", "szczypta")), 500, 10, 20, 4);
        final Portion anna = new Portion("Anna", List.of(
                new Ingredient("jajka", "1 szt."),
                new Ingredient("masło", "5 g")), 200, 4, 8, 2);
        final WeekPlanRepair repair = new WeekPlanRepair(new WeekPlanValidator());

        final WeekPlan actual = repair.repair(new WeekPlan(IntStream.range(0, 7)
                .mapToObj((final int index) -> new DayMeals(
                        new Meal("Sniadanie " + index, "Kroki.", List.of(ja, anna)),
                        new Meal("Jogurt", "Kroki.", List.of(plain("Ja", 0), plain("Anna", 0))),
                        new Meal("Obiad " + index, "Kroki.", List.of(plain("Ja", 0), plain("Anna", 0))),
                        new Meal("Kolacja " + index, "Kroki.", List.of(
                                new Portion("Ja", List.of(new Ingredient("oliwa", "1 łyżka")), 0, 0, 0, 0),
                                plain("Anna", 0)))))
                .toList(), List.of(new ShoppingItem("Jajka", "1 szt.", "Nabiał"), new ShoppingItem("jogurt", "7 szt."))),
                people);

        assertThat(actual.shoppingList()).usingRecursiveComparison().isEqualTo(List.of(
                new ShoppingItem("jajka", "21 szt.", "Nabiał"),
                new ShoppingItem("masło", "105 g", ""),
                new ShoppingItem("sól", "7× szczypta", ""),
                new ShoppingItem("oliwa", "7 łyżka", "")));
    }

    @Test
    void shouldReturnNullWhenAScaledMacroDoesNotFitInAnInteger() {
        final List<PlanPerson> people = List.of(new PlanPerson("Ja", Goal.MAINTAIN, 2000));
        final DayMeals day = new DayMeals(
                new Meal("Sniadanie", "", List.of(new Portion("Ja", List.of(), 1, Integer.MAX_VALUE, 0, 0))),
                new Meal("Jogurt", "", List.of(plain("Ja", 0))),
                new Meal("Obiad", "", List.of(plain("Ja", 0))),
                new Meal("Kolacja", "", List.of(plain("Ja", 0))));
        final WeekPlanRepair repair = new WeekPlanRepair(new WeekPlanValidator());

        final DayMeals actual = repair.scaleDay(day, people);

        assertThat(actual).isNull();
    }

    @Test
    void shouldSumQuartersOfALemonIntoOneAndAHalf() {
        final Ingredient lemon = new Ingredient("cytryna", "1/4 szt.");
        final Meal lunch = new Meal("Kurczak", "Kroki.", List.of(
                new Portion("Ja", List.of(lemon), 1, 0, 0, 0),
                new Portion("Anna", List.of(lemon), 1, 0, 0, 0),
                new Portion("Ola", List.of(lemon), 1, 0, 0, 0)));
        final Meal empty = new Meal("Puste", "", List.of());
        final DayMeals day = new DayMeals(empty, empty, lunch, empty);
        final List<ShoppingItem> actual = new WeekPlanRepair(new WeekPlanValidator())
                .shoppingList(List.of(day, day), List.of());

        assertThat(actual).usingRecursiveComparison().isEqualTo(List.of(new ShoppingItem("cytryna", "1.5 szt.")));
    }

    @Test
    void shouldReturnNullWhenAMealHasTheWrongNumberOfPortions() {
        final List<PlanPerson> people = List.of(
                new PlanPerson("Ja", Goal.MAINTAIN, 2000),
                new PlanPerson("Anna", Goal.LOSE_WEIGHT, 800));
        final WeekPlanRepair repair = new WeekPlanRepair(new WeekPlanValidator());

        final WeekPlan actual = repair.repair(week(portion("Ja", 500)), people);

        assertThat(actual).isNull();
    }

    private DayMeals scaledDay(final int index) {
        return new DayMeals(
                new Meal("Sniadanie " + index, "Kroki.", List.of(
                        portion("Ja", 500, 50, 39, 16), portion("Anna", 200, 20, 17, 6))),
                new Meal("Jogurt", "Kroki.", List.of(
                        portion("Ja", 500, 50, 37, 17), portion("Anna", 200, 20, 14, 7))),
                new Meal("Obiad " + index, "Kroki.", List.of(
                        portion("Ja", 500, 50, 37, 17), portion("Anna", 200, 20, 14, 7))),
                new Meal("Kolacja " + index, "Kroki.", List.of(
                        portion("Ja", 500, 50, 37, 17), portion("Anna", 200, 20, 14, 7))));
    }

    private WeekPlan week(final Portion... mealPortions) {
        return new WeekPlan(IntStream.range(0, 7)
                .mapToObj((final int index) -> new DayMeals(
                        new Meal("Sniadanie " + index, "Kroki.", List.copyOf(Arrays.asList(mealPortions))),
                        new Meal("Jogurt", "Kroki.", List.copyOf(Arrays.asList(mealPortions))),
                        new Meal("Obiad " + index, "Kroki.", List.copyOf(Arrays.asList(mealPortions))),
                        new Meal("Kolacja " + index, "Kroki.", List.copyOf(Arrays.asList(mealPortions)))))
                .toList(), List.of(new ShoppingItem("jogurt", "7 szt.")));
    }

    private Portion plain(final String person, final int kcal) {
        return new Portion(person, List.of(), kcal, 0, 0, 0);
    }

    private Portion portion(final String person, final int kcal) {
        return portion(person, kcal, 10, 20, 4);
    }

    private Portion portion(final String person, final int kcal, final int protein, final int carbs, final int fat) {
        return new Portion(person, List.of(new Ingredient("jajka", "2 szt.")), kcal, protein, carbs, fat);
    }
}
