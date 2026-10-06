package com.kenez92.plateplan.plan.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.kenez92.plateplan.plan.model.DietPlan;
import com.kenez92.plateplan.plan.model.Meal;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlanPdfWriterTest {

    @Test
    void shouldRenderPolishGlyphsFourMealsAndTomorrowsDateInTheDietPdf() throws IOException {
        final String actual = text(new PlanPdfWriter().dietPdf(samplePlan(), 2000));

        assertThat(actual).contains("Żółć", "jądro", "Śniadanie", "Drugie śniadanie", "Obiad", "Kolacja",
                "Cel dnia: 2000 kcal", "400 kcal", "200 kcal", "800 kcal", "600 kcal",
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
    void shouldRenderPolishGlyphsAndShoppingItemsInTheShoppingListPdf() throws IOException {
        final String actual = text(new PlanPdfWriter().shoppingListPdf(samplePlan()));

        assertThat(actual).contains("Lista zakupów", "Żółć", "jądro", "chleb");
        assertThat(actual).doesNotContain("alice");
    }

    private String text(final byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private DietPlan samplePlan() {
        return new DietPlan(
                new Meal("Żółć", List.of("jądro"), 400),
                new Meal("Jogurt", List.of("skyr"), 200),
                new Meal("Schabowy", List.of("schab"), 800),
                new Meal("Zupa", List.of("warzywa"), 600),
                List.of("Żółć", "jądro", "chleb"));
    }
}
