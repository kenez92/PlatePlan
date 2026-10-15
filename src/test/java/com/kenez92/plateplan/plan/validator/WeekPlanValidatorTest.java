package com.kenez92.plateplan.plan.validator;

import java.util.List;
import java.util.stream.IntStream;

import com.kenez92.plateplan.plan.model.DayMeals;
import com.kenez92.plateplan.plan.model.Meal;
import com.kenez92.plateplan.plan.model.PlanPerson;
import com.kenez92.plateplan.plan.model.Portion;
import com.kenez92.plateplan.plan.model.WeekPlan;
import com.kenez92.plateplan.profile.model.Goal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WeekPlanValidatorTest {

    @Test
    void shouldAcceptDayTotalsAtBothEndsOfTheBandWhenTheTargetIs2200() {
        final WeekPlanValidator validator = validator();

        assertThat(validator.isValid(weekTotalling(7, 1980), target(2200))).isTrue();
        assertThat(validator.isValid(weekTotalling(7, 2420), target(2200))).isTrue();
    }

    @Test
    void shouldRejectDayTotalsJustOutsideTheBandWhenTheTargetIs2200() {
        final WeekPlanValidator validator = validator();

        assertThat(validator.isValid(weekTotalling(7, 1979), target(2200))).isFalse();
        assertThat(validator.isValid(weekTotalling(7, 2421), target(2200))).isFalse();
    }

    @Test
    void shouldAcceptDayTotalsAtBothEndsOfTheBandWhenTheTargetIs1999() {
        final WeekPlanValidator validator = validator();

        assertThat(validator.isValid(weekTotalling(7, 1799), target(1999))).isTrue();
        assertThat(validator.isValid(weekTotalling(7, 2199), target(1999))).isTrue();
    }

    @Test
    void shouldRejectDayTotalsJustOutsideTheBandWhenTheTargetIs1999() {
        final WeekPlanValidator validator = validator();

        assertThat(validator.isValid(weekTotalling(7, 1798), target(1999))).isFalse();
        assertThat(validator.isValid(weekTotalling(7, 2200), target(1999))).isFalse();
    }

    @Test
    void shouldAcceptEachSelectedPersonAtTheEndsOfThatPersonsBand() {
        final List<PlanPerson> people = List.of(
                new PlanPerson("Ja", Goal.MAINTAIN, 795),
                new PlanPerson("Anna", Goal.MAINTAIN, 400),
                new PlanPerson("Ola", Goal.GAIN, 6000));
        final List<String> names = List.of("Ja", "Anna", "Ola");

        assertThat(validator().isValid(weekFor(names, List.of(715, 360, 5400)), people)).isTrue();
        assertThat(validator().isValid(weekFor(names, List.of(875, 440, 6600)), people)).isTrue();
    }

    @Test
    void shouldRejectWhenOneSelectedPersonFallsOutsideThatPersonsBand() {
        final WeekPlanValidator validator = validator();
        final List<PlanPerson> people = List.of(
                new PlanPerson("Ja", Goal.MAINTAIN, 795),
                new PlanPerson("Anna", Goal.MAINTAIN, 2200));
        final List<String> names = List.of("Ja", "Anna");

        assertThat(validator.isValid(weekFor(names, List.of(714, 2200)), people)).isFalse();
        assertThat(validator.isValid(weekFor(names, List.of(876, 2200)), people)).isFalse();
        assertThat(validator.isValid(weekFor(names, List.of(795, 1979)), people)).isFalse();
    }

    @Test
    void shouldRejectWhenASelectedPersonHasNoPortion() {
        final boolean actual = validator().isValid(
                weekFor(List.of("Ja"), List.of(2200)),
                List.of(new PlanPerson("Ja", Goal.MAINTAIN, 2200), new PlanPerson("Anna", Goal.MAINTAIN, 795)));

        assertThat(actual).isFalse();
    }

    @Test
    void shouldRejectWhenAMealHasAPortionForSomeoneElse() {
        final boolean actual = validator().isValid(
                weekFor(List.of("Ja", "Anna"), List.of(2200, 795)),
                List.of(new PlanPerson("Ja", Goal.MAINTAIN, 2200)));

        assertThat(actual).isFalse();
    }

    @Test
    void shouldRejectTwoPortionsForTheSamePerson() {
        final WeekPlan week = new WeekPlan(IntStream.range(0, 7)
                .mapToObj((final int index) -> new DayMeals(
                        new Meal("Sniadanie " + index, "", List.of(
                                new Portion("Ja", List.of(), 2197, 0, 0, 0),
                                new Portion("ja", List.of(), 0, 0, 0, 0))),
                        meal("Drugie sniadanie " + index, 1),
                        meal("Obiad " + index, 1),
                        meal("Kolacja " + index, 1)))
                .toList(), List.of());

        assertThat(validator().isValid(week, target(2200))).isFalse();
    }

    @Test
    void shouldAcceptAPortionNameThatDiffersOnlyByCaseOrEdgeSpaces() {
        final WeekPlan week = new WeekPlan(IntStream.range(0, 7)
                .mapToObj((final int index) -> new DayMeals(
                        portionMeal("Sniadanie " + index, " ja ", 2200),
                        portionMeal("Drugie sniadanie " + index, "JA", 0),
                        portionMeal("Obiad " + index, "Ja", 0),
                        portionMeal("Kolacja " + index, " Ja ", 0)))
                .toList(), List.of());

        assertThat(validator().isValid(week, target(2200))).isTrue();
    }

    @Test
    void shouldRejectTheSameNameOnBreakfastAndLunch() {
        final boolean actual = validator().isValid(
                weekWithBreakfastAndLunch("Jogurt naturalny", "Jogurt naturalny"), target(2200));

        assertThat(actual).isFalse();
    }

    @Test
    void shouldRejectABreakfastAndLunchThatDifferOnlyByCaseOrEdgeSpaces() {
        final WeekPlanValidator validator = validator();

        assertThat(validator.isValid(weekWithBreakfastAndLunch("Jogurt naturalny", "JOGURT NATURALNY"), target(2200)))
                .isFalse();
        assertThat(validator.isValid(weekWithBreakfastAndLunch("Jogurt naturalny", " Jogurt naturalny "), target(2200)))
                .isFalse();
    }

    @Test
    void shouldAcceptTheSameSecondBreakfastOnAllSevenDays() {
        final boolean actual = validator().isValid(new WeekPlan(IntStream.range(0, 7)
                .mapToObj((final int index) -> day(index, 2200, "Jajecznica"))
                .toList(), List.of()), target(2200));

        assertThat(actual).isTrue();
    }

    @Test
    void shouldAcceptJogurtAsBreakfastOnDayOneAndAsSecondBreakfastOnDayThree() {
        final boolean actual = validator().isValid(new WeekPlan(IntStream.range(0, 7)
                .mapToObj((final int index) -> new DayMeals(
                        meal(index == 0 ? "Jogurt naturalny" : "Sniadanie " + index, 2200),
                        meal(index == 2 ? "Jogurt naturalny" : "Drugie sniadanie " + index, 0),
                        meal("Obiad " + index, 0),
                        meal("Kolacja " + index, 0)))
                .toList(), List.of()), target(2200));

        assertThat(actual).isTrue();
    }

    @Test
    void shouldRejectSixDaysAndEightDays() {
        final WeekPlanValidator validator = validator();

        assertThat(validator.isValid(week(6, 2200), target(2200))).isFalse();
        assertThat(validator.isValid(week(8, 2200), target(2200))).isFalse();
    }

    @Test
    void shouldRejectAnEmptyMealName() {
        final WeekPlanValidator validator = validator();

        assertThat(validator.isValid(weekWithBreakfastAndLunch("", "Obiad 0"), target(2200))).isFalse();
        assertThat(validator.isValid(weekWithBreakfastAndLunch("   ", "Obiad 0"), target(2200))).isFalse();
        assertThat(validator.isValid(weekWithSecondBreakfast(""), target(2200))).isFalse();
    }

    @Test
    void shouldAcceptAnEmptyShoppingList() {
        final WeekPlanValidator validator = validator();
        final List<DayMeals> days = days(7, 2200);

        assertThat(validator.isValid(new WeekPlan(days, List.of()), target(2200))).isTrue();
        assertThat(validator.isValid(new WeekPlan(days, null), target(2200))).isTrue();
    }

    @Test
    void shouldRejectAWeekWhoseDaysAreMissing() {
        final boolean actual = validator().isValid(new WeekPlan(null, List.of()), target(2200));

        assertThat(actual).isFalse();
    }

    private WeekPlanValidator validator() {
        return new WeekPlanValidator();
    }

    private List<PlanPerson> target(final int kcal) {
        return List.of(new PlanPerson("Ja", Goal.MAINTAIN, kcal));
    }

    private WeekPlan week(final int dayCount, final int kcal) {
        return new WeekPlan(days(dayCount, kcal), List.of());
    }

    private WeekPlan weekTotalling(final int dayCount, final int total) {
        return new WeekPlan(IntStream.range(0, dayCount)
                .mapToObj((final int index) -> dayTotalling(index, total))
                .toList(), List.of());
    }

    private WeekPlan weekFor(final List<String> names, final List<Integer> totals) {
        return new WeekPlan(IntStream.range(0, 7)
                .mapToObj((final int index) -> dayFor(index, names, totals))
                .toList(), List.of());
    }

    private WeekPlan weekWithBreakfastAndLunch(final String breakfast, final String lunch) {
        return new WeekPlan(IntStream.range(0, 7)
                .mapToObj((final int index) -> index == 0
                        ? new DayMeals(
                                meal(breakfast, 2200),
                                meal("Drugie sniadanie 0", 0),
                                meal(lunch, 0),
                                meal("Kolacja 0", 0))
                        : day(index, 2200, "Drugie sniadanie " + index))
                .toList(), List.of());
    }

    private WeekPlan weekWithSecondBreakfast(final String secondBreakfast) {
        return new WeekPlan(IntStream.range(0, 7)
                .mapToObj((final int index) -> day(index, 2200,
                        index == 0 ? secondBreakfast : "Drugie sniadanie " + index))
                .toList(), List.of());
    }

    private List<DayMeals> days(final int dayCount, final int kcal) {
        return IntStream.range(0, dayCount)
                .mapToObj((final int index) -> day(index, kcal, "Drugie sniadanie " + index))
                .toList();
    }

    private DayMeals day(final int index, final int kcal, final String secondBreakfast) {
        return new DayMeals(
                meal("Sniadanie " + index, kcal),
                meal(secondBreakfast, 0),
                meal("Obiad " + index, 0),
                meal("Kolacja " + index, 0));
    }

    private DayMeals dayTotalling(final int index, final int total) {
        return new DayMeals(
                meal("Sniadanie " + index, total - 3),
                meal("Drugie sniadanie " + index, 1),
                meal("Obiad " + index, 1),
                meal("Kolacja " + index, 1));
    }

    private DayMeals dayFor(final int index, final List<String> names, final List<Integer> totals) {
        return new DayMeals(
                mealFor("Sniadanie " + index, names, breakfastKcal(totals)),
                mealFor("Drugie sniadanie " + index, names, ones(names.size())),
                mealFor("Obiad " + index, names, ones(names.size())),
                mealFor("Kolacja " + index, names, ones(names.size())));
    }

    private Meal mealFor(final String name, final List<String> names, final List<Integer> kcal) {
        return new Meal(name, "", IntStream.range(0, names.size())
                .mapToObj((final int index) -> new Portion(names.get(index), List.of(), kcal.get(index), 0, 0, 0))
                .toList());
    }

    private List<Integer> breakfastKcal(final List<Integer> totals) {
        return totals.stream().map((final Integer total) -> total - 3).toList();
    }

    private List<Integer> ones(final int count) {
        return IntStream.range(0, count).mapToObj((final int index) -> 1).toList();
    }

    private Meal meal(final String name, final int kcal) {
        return portionMeal(name, "Ja", kcal);
    }

    private Meal portionMeal(final String name, final String person, final int kcal) {
        return new Meal(name, "", List.of(new Portion(person, List.of(), kcal, 0, 0, 0)));
    }
}
