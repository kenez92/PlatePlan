package com.kenez92.plateplan.plan.validator;

import java.util.List;
import java.util.stream.IntStream;

import com.kenez92.plateplan.plan.model.DayMeals;
import com.kenez92.plateplan.plan.model.Meal;
import com.kenez92.plateplan.plan.model.WeekPlan;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WeekPlanValidatorTest {

    @Test
    void shouldAcceptDayTotalsAtBothEndsOfTheBandWhenTheTargetIs2200() {
        final WeekPlanValidator validator = validator();

        assertThat(validator.isValid(weekTotalling(7, 1980), 2200)).isTrue();
        assertThat(validator.isValid(weekTotalling(7, 2420), 2200)).isTrue();
    }

    @Test
    void shouldRejectDayTotalsJustOutsideTheBandWhenTheTargetIs2200() {
        final WeekPlanValidator validator = validator();

        assertThat(validator.isValid(weekTotalling(7, 1979), 2200)).isFalse();
        assertThat(validator.isValid(weekTotalling(7, 2421), 2200)).isFalse();
    }

    @Test
    void shouldAcceptDayTotalsAtBothEndsOfTheBandWhenTheTargetIs1999() {
        final WeekPlanValidator validator = validator();

        assertThat(validator.isValid(weekTotalling(7, 1799), 1999)).isTrue();
        assertThat(validator.isValid(weekTotalling(7, 2199), 1999)).isTrue();
    }

    @Test
    void shouldRejectDayTotalsJustOutsideTheBandWhenTheTargetIs1999() {
        final WeekPlanValidator validator = validator();

        assertThat(validator.isValid(weekTotalling(7, 1798), 1999)).isFalse();
        assertThat(validator.isValid(weekTotalling(7, 2200), 1999)).isFalse();
    }

    @Test
    void shouldRejectTheSameNameOnBreakfastAndLunch() {
        final boolean actual = validator().isValid(
                weekWithBreakfastAndLunch("Jogurt naturalny", "Jogurt naturalny"), 2200);

        assertThat(actual).isFalse();
    }

    @Test
    void shouldRejectABreakfastAndLunchThatDifferOnlyByCaseOrEdgeSpaces() {
        final WeekPlanValidator validator = validator();

        assertThat(validator.isValid(weekWithBreakfastAndLunch("Jogurt naturalny", "JOGURT NATURALNY"), 2200))
                .isFalse();
        assertThat(validator.isValid(weekWithBreakfastAndLunch("Jogurt naturalny", " Jogurt naturalny "), 2200))
                .isFalse();
    }

    @Test
    void shouldAcceptTheSameSecondBreakfastOnAllSevenDays() {
        final boolean actual = validator().isValid(new WeekPlan(IntStream.range(0, 7)
                .mapToObj((final int index) -> day(index, 2200, "Jajecznica"))
                .toList(), List.of()), 2200);

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
                .toList(), List.of()), 2200);

        assertThat(actual).isTrue();
    }

    @Test
    void shouldRejectSixDaysAndEightDays() {
        final WeekPlanValidator validator = validator();

        assertThat(validator.isValid(week(6, 2200), 2200)).isFalse();
        assertThat(validator.isValid(week(8, 2200), 2200)).isFalse();
    }

    @Test
    void shouldRejectAnEmptyMealName() {
        final WeekPlanValidator validator = validator();

        assertThat(validator.isValid(weekWithBreakfastAndLunch("", "Obiad 0"), 2200)).isFalse();
        assertThat(validator.isValid(weekWithBreakfastAndLunch("   ", "Obiad 0"), 2200)).isFalse();
        assertThat(validator.isValid(weekWithSecondBreakfast(""), 2200)).isFalse();
    }

    @Test
    void shouldAcceptAnEmptyShoppingList() {
        final WeekPlanValidator validator = validator();
        final List<DayMeals> days = days(7, 2200);

        assertThat(validator.isValid(new WeekPlan(days, List.of()), 2200)).isTrue();
        assertThat(validator.isValid(new WeekPlan(days, null), 2200)).isTrue();
    }

    @Test
    void shouldRejectAWeekWhoseDaysAreMissing() {
        final boolean actual = validator().isValid(new WeekPlan(null, List.of()), 2200);

        assertThat(actual).isFalse();
    }

    private WeekPlanValidator validator() {
        return new WeekPlanValidator();
    }

    private WeekPlan week(final int dayCount, final int kcal) {
        return new WeekPlan(days(dayCount, kcal), List.of());
    }

    private WeekPlan weekTotalling(final int dayCount, final int total) {
        return new WeekPlan(IntStream.range(0, dayCount)
                .mapToObj((final int index) -> dayTotalling(index, total))
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

    private Meal meal(final String name, final int kcal) {
        return new Meal(name, List.of(), kcal, 0, 0, 0, "");
    }
}
