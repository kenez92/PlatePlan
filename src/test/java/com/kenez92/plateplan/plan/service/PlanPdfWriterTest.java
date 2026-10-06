package com.kenez92.plateplan.plan.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.kenez92.plateplan.plan.model.DietPlan;
import com.kenez92.plateplan.plan.model.Ingredient;
import com.kenez92.plateplan.plan.model.Meal;
import com.kenez92.plateplan.plan.model.ShoppingItem;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlanPdfWriterTest {

    @Test
    void shouldRenderPolishGlyphsFourMealsAndTomorrowsDateInTheDietPdf() throws IOException {
        final String actual = text(new PlanPdfWriter().dietPdf(samplePlan(), 2000));

        assertThat(actual).contains("Żółć", "jądro", "100 g", "Śniadanie", "Drugie śniadanie", "Obiad", "Kolacja",
                "Cel dnia: 2000 kcal", "400 kcal", "200 kcal", "800 kcal", "600 kcal",
                "Białko 20 g", "Węglowodany 10 g", "Tłuszcz 15 g",
                "Białko 90 g", "Węglowodany 108 g", "Tłuszcz 72 g",
                "Składniki", "Przygotowanie", "Sparzyć żółć.",
                LocalDate.now(ZoneId.of("Europe/Warsaw")).plusDays(1).format(DateTimeFormatter.ofPattern("d.MM.yyyy")));
        assertThat(actual).doesNotContain("alice");
    }

    @Test
    void shouldPrintThePassedDailyTargetNotTheSumOfMeals() throws IOException {
        final String actual = text(new PlanPdfWriter().dietPdf(samplePlan(), 1850));

        assertThat(actual).contains("Cel dnia: 1850 kcal");
        assertThat(actual).doesNotContain("Cel dnia: 2000 kcal");
    }

    @Test
    void shouldWrapALongIngredientWithoutThrowing() throws IOException {
        final String longName = "abcdefghijklmnopqrstuvwxyz ".repeat(8).strip();
        final DietPlan plan = new DietPlan(
                new Meal("Żółć", List.of(new Ingredient(longName, "1 szt.")), 400, 20, 10, 15, "Sparzyć żółć."),
                new Meal("Jogurt", List.of(new Ingredient("skyr", "200 g")), 200, 18, 8, 2, "Otwórz skyr."),
                new Meal("Schabowy", List.of(new Ingredient("schab", "150 g")), 800, 40, 50, 35, "Usmaż schab."),
                new Meal("Zupa", List.of(new Ingredient("warzywa", "300 g")), 600, 12, 40, 20,
                        "Gotuj warzywa do miękkości."),
                List.of(new ShoppingItem(longName, "1 szt.")));

        assertThat(text(new PlanPdfWriter().dietPdf(plan, 2000))).contains("abcdefghijklmnopqrstuvwxyz");
        assertThat(text(new PlanPdfWriter().shoppingListPdf(plan))).contains("abcdefghijklmnopqrstuvwxyz");
    }

    @Test
    void shouldRenderPolishGlyphsAndShoppingItemsInTheShoppingListPdf() throws IOException {
        final String actual = text(new PlanPdfWriter().shoppingListPdf(samplePlan()));

        assertThat(actual).contains("Lista zakupów", "Żółć", "100 g", "jądro", "chleb", "60 g");
        assertThat(actual).doesNotContain("alice");
    }

    private String text(final byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private DietPlan samplePlan() {
        return new DietPlan(
                new Meal("Żółć", List.of(new Ingredient("jądro", "100 g")), 400, 20, 10, 15, "Sparzyć żółć."),
                new Meal("Jogurt", List.of(new Ingredient("skyr", "200 g")), 200, 18, 8, 2, "Otwórz skyr."),
                new Meal("Schabowy", List.of(new Ingredient("schab", "150 g")), 800, 40, 50, 35, "Usmaż schab."),
                new Meal("Zupa", List.of(new Ingredient("warzywa", "300 g")), 600, 12, 40, 20,
                        "Gotuj warzywa do miękkości."),
                List.of(
                        new ShoppingItem("Żółć", "100 g"),
                        new ShoppingItem("jądro", "100 g"),
                        new ShoppingItem("chleb", "60 g")));
    }
}
