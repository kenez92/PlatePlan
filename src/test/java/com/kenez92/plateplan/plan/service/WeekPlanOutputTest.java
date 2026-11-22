package com.kenez92.plateplan.plan.service;

import java.util.List;

import com.kenez92.plateplan.plan.model.DayMeals;
import com.kenez92.plateplan.plan.model.Ingredient;
import com.kenez92.plateplan.plan.model.Meal;
import com.kenez92.plateplan.plan.model.Portion;
import com.kenez92.plateplan.plan.model.ShoppingItem;
import com.kenez92.plateplan.plan.model.WeekPlan;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WeekPlanOutputTest {

    @Test
    void shouldKeepFinishedDaysWhenTheReplyIsCutOff() {
        final String json = "{\"days\":[" + day("Jeden", "1.6", "150") + "," + day("Dwa", "2", "\"2 szt.\"")
                + ",{\"breakfast\":{\"name\":\"Urwane\"";
        final WeekPlanOutput output = new WeekPlanOutput();

        final WeekPlan actual = output.convert(json);

        final WeekPlan expected = new WeekPlan(List.of(expected("Jeden", 2, "150"), expected("Dwa", 2, "2 szt.")),
                List.of());
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldKeepACommaWrittenInsideThePreparation() {
        final String json = "{\"days\":[" + dayWithPreparation("Kroki,}") + "],\"shoppingList\":[]}";
        final WeekPlanOutput output = new WeekPlanOutput();

        final WeekPlan actual = output.convert(json);

        assertThat(actual.days().getFirst().breakfast().preparation()).isEqualTo("Kroki,}");
    }

    @Test
    void shouldReadAWeekWrappedInAMarkdownFence() {
        final String json = "```json\n{\"days\":[" + day("Jeden", "2", "\"1 szt.\"")
                + "],\"shoppingList\":[{\"name\":\"jajka\",\"amount\":\"1 szt.\",}]}\n```";
        final WeekPlanOutput output = new WeekPlanOutput();

        final WeekPlan actual = output.convert(json);

        final WeekPlan expected = new WeekPlan(List.of(expected("Jeden", 2, "1 szt.")),
                List.of(new ShoppingItem("jajka", "1 szt.")));
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    private String dayWithPreparation(final String preparation) {
        return "{\"breakfast\":{\"name\":\"Jeden\",\"preparation\":\"" + preparation + "\",\"portions\":["
                + "{\"personName\":\"Ja\",\"kcal\":1,\"proteinG\":0,\"carbsG\":0,\"fatG\":0}]},"
                + "\"secondBreakfast\":{\"name\":\"Jogurt\",\"preparation\":\"\",\"portions\":["
                + "{\"personName\":\"Ja\",\"kcal\":1,\"proteinG\":0,\"carbsG\":0,\"fatG\":0}]},"
                + "\"lunch\":{\"name\":\"Obiad\",\"preparation\":\"\",\"portions\":["
                + "{\"personName\":\"Ja\",\"kcal\":1,\"proteinG\":0,\"carbsG\":0,\"fatG\":0}]},"
                + "\"dinner\":{\"name\":\"Kolacja\",\"preparation\":\"\",\"portions\":["
                + "{\"personName\":\"Ja\",\"kcal\":1,\"proteinG\":0,\"carbsG\":0,\"fatG\":0}]}}";
    }

    private String day(final String name, final String breakfastKcal, final String amount) {
        return "{\"breakfast\":{\"name\":\"" + name + "\",\"preparation\":\"Kroki.\",\"portions\":[{\"personName\":\"Ja\","
                + "\"ingredients\":[{\"name\":\"jajka\",\"amount\":" + amount + "}],\"kcal\":" + breakfastKcal
                + ",\"proteinG\":0,\"carbsG\":0,\"fatG\":0,}]},\"secondBreakfast\":{\"name\":\"Jogurt\",\"preparation\":\"\","
                + "\"portions\":[{\"personName\":\"Ja\",\"kcal\":1,\"proteinG\":0,\"carbsG\":0,\"fatG\":0}]},"
                + "\"lunch\":{\"name\":\"Obiad " + name + "\",\"preparation\":\"\",\"portions\":[{\"personName\":\"Ja\","
                + "\"kcal\":\"1.5\",\"proteinG\":0,\"carbsG\":0,\"fatG\":0}]},\"dinner\":{\"name\":\"Kolacja " + name
                + "\",\"preparation\":\"\",\"portions\":[{\"personName\":\"Ja\",\"kcal\":1,\"proteinG\":0,\"carbsG\":0,"
                + "\"fatG\":0}]}}";
    }

    private DayMeals expected(final String name, final int breakfastKcal, final String amount) {
        return new DayMeals(
                new Meal(name, "Kroki.", List.of(new Portion("Ja", List.of(new Ingredient("jajka", amount)),
                        breakfastKcal, 0, 0, 0))),
                new Meal("Jogurt", "", List.of(new Portion("Ja", List.of(), 1, 0, 0, 0))),
                new Meal("Obiad " + name, "", List.of(new Portion("Ja", List.of(), 2, 0, 0, 0))),
                new Meal("Kolacja " + name, "", List.of(new Portion("Ja", List.of(), 1, 0, 0, 0))));
    }
}
