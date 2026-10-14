package com.kenez92.plateplan.plan.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.IntStream;

import com.kenez92.plateplan.plan.model.DayMeals;
import com.kenez92.plateplan.plan.model.Ingredient;
import com.kenez92.plateplan.plan.model.Meal;
import com.kenez92.plateplan.plan.model.ShoppingItem;
import com.kenez92.plateplan.plan.model.WeekPlan;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlanPdfWriterTest {

    @Test
    void shouldRenderPolishGlyphsAndFourMealsInTheDietPdf() throws IOException {
        final String actual = text(new PlanPdfWriter().dietPdf(samplePlan(), 2000));

        assertThat(actual).contains("Żółć", "jądro", "100 g", "Śniadanie", "Drugie śniadanie", "Obiad", "Kolacja",
                "Dzień 1",
                "Cel dnia: 2000 kcal", "400 kcal", "200 kcal", "800 kcal", "600 kcal",
                "Białko 20 g", "Węglowodany 10 g", "Tłuszcz 15 g",
                "Białko 90 g", "Węglowodany 108 g", "Tłuszcz 72 g",
                "Składniki", "Przygotowanie", "Sparzyć żółć.");
        assertThat(actual).doesNotContain("alice", "Jadłospis",
                LocalDate.now(ZoneId.of("Europe/Warsaw")).plusDays(1).format(DateTimeFormatter.ofPattern("d.MM.yyyy")));
    }

    @Test
    void shouldRenderEachDayInOrderWithThatDaysMacroSum() throws IOException {
        final WeekPlan week = new WeekPlan(List.of(
                new DayMeals(
                        new Meal("Owsianka", List.of(new Ingredient("platki", "50 g")), 400, 10, 20, 5, "Zagotuj."),
                        new Meal("Jogurt", List.of(new Ingredient("skyr", "200 g")), 200, 18, 8, 2, "Otwórz skyr."),
                        new Meal("Schabowy", List.of(new Ingredient("schab", "150 g")), 800, 40, 50, 35, "Usmaż schab."),
                        new Meal("Zupa", List.of(new Ingredient("warzywa", "300 g")), 600, 12, 40, 20,
                                "Gotuj warzywa do miękkości.")),
                new DayMeals(
                        new Meal("Omlet", List.of(new Ingredient("jajka", "2 szt.")), 300, 20, 2, 22, "Usmaż omlet."),
                        new Meal("Twaróg", List.of(new Ingredient("twarog", "150 g")), 150, 20, 4, 5, "Wyłóż twaróg."),
                        new Meal("Ryba", List.of(new Ingredient("dorsz", "150 g")), 400, 35, 0, 10, "Upiecz rybę."),
                        new Meal("Kasza", List.of(new Ingredient("kasza", "80 g")), 350, 10, 60, 5, "Ugotuj kaszę."))),
                List.of(new ShoppingItem("platki", "50 g")));

        final String diet = text(new PlanPdfWriter().dietPdf(week, 2000));
        final String shoppingList = text(new PlanPdfWriter().shoppingListPdf(week));

        assertThat(diet).containsSubsequence("Dzień 1", "Owsianka", "Jogurt", "Schabowy", "Zupa",
                "Dzień 2", "Omlet", "Twaróg", "Ryba", "Kasza");
        assertThat(diet).contains("Białko 80 g", "Białko 85 g");
        assertThat(diet).doesNotContain("Jadłospis");
        assertThat(shoppingList).containsOnlyOnce("Lista zakupów");
        assertThat(shoppingList).containsOnlyOnce("platki");
        assertThat(shoppingList).doesNotContain("Owsianka", "Omlet", "Dzień");
    }

    @Test
    void shouldPrintThePassedDailyTargetNotTheSumOfMeals() throws IOException {
        final String actual = text(new PlanPdfWriter().dietPdf(samplePlan(), 1850));

        assertThat(actual).contains("Dzień 1", "Cel dnia: 1850 kcal");
        assertThat(actual).doesNotContain("Cel dnia: 2000 kcal", "Jadłospis");
    }

    @Test
    void shouldGiveAShortWeekSevenPagesTitledByDayNumber() throws IOException {
        final LocalDate tomorrow = LocalDate.now(ZoneId.of("Europe/Warsaw")).plusDays(1);
        final DateTimeFormatter format = DateTimeFormatter.ofPattern("d.MM.yyyy");
        final List<String> pages = pagesOf(new PlanPdfWriter().dietPdf(shortWeek(), 1850));

        assertThat(pages).hasSize(7);
        assertThat(pages).allSatisfy((final String page) -> assertThat(page.strip()).isNotEmpty());
        IntStream.range(0, pages.size()).forEach((final int index) -> assertThat(pages.get(index))
                .contains("Dzień " + (index + 1), "Cel dnia: 1850 kcal")
                .doesNotContain(tomorrow.plusDays(index).format(format), "Cel dnia: 2000 kcal", "Jadłospis"));
        assertThat(pages.get(0)).contains("Owsianka 1").doesNotContain("Dzień 2", "Owsianka 7");
        assertThat(pages.get(6)).contains("Dzień 7", "Owsianka 7")
                .doesNotContain(tomorrow.plusDays(6).format(format), "Dzień 1", "Owsianka 1");
    }

    @Test
    void shouldStartTheNextDayOnANewPageWhenADayOverflows() throws IOException {
        final List<String> pages = pagesOf(new PlanPdfWriter().dietPdf(weekWithALongFirstDay(), 1850));
        final int nextDay = IntStream.range(0, pages.size())
                .filter((final int index) -> pages.get(index).contains("Dzień 2"))
                .findFirst()
                .orElseThrow();

        assertThat(pages.size()).isGreaterThan(7);
        assertThat(pages).allSatisfy((final String page) -> assertThat(page.strip()).isNotEmpty());
        assertThat(pages.get(0)).contains("Dzień 1", "Cel dnia: 1850 kcal");
        assertThat(pages.get(0)).doesNotContain("Dzień 2", "koniec-dnia");
        assertThat(pages.get(nextDay).strip()).startsWith("Dzień 2");
        assertThat(pages.get(nextDay)).doesNotContain("koniec-dnia", "Dzień 1");
        assertThat(pages.get(nextDay - 1)).contains("koniec-dnia");
        assertThat(pages.get(nextDay - 1)).doesNotContain("Dzień 2");
    }

    @Test
    void shouldKeepADayWithTemperaturesAndTimesOnASinglePage() throws IOException {
        final List<String> pages = pagesOf(new PlanPdfWriter().dietPdf(detailedWeek(), 1850));

        assertThat(pages).hasSize(7);
        assertThat(pages).allSatisfy((final String page) -> assertThat(page.strip()).isNotEmpty());
        assertThat(pages.get(0)).contains("Dzień 1", "180 °C", "25 minut").doesNotContain("Dzień 2");
        assertThat(pages.get(6)).contains("Dzień 7", "180 °C").doesNotContain("Dzień 1");
    }

    @Test
    void shouldWrapALongIngredientWithoutThrowing() throws IOException {
        final String longName = "abcdefghijklmnopqrstuvwxyz ".repeat(8).strip();
        final WeekPlan plan = new WeekPlan(List.of(new DayMeals(
                new Meal("Żółć", List.of(new Ingredient(longName, "1 szt.")), 400, 20, 10, 15, "Sparzyć żółć."),
                new Meal("Jogurt", List.of(new Ingredient("skyr", "200 g")), 200, 18, 8, 2, "Otwórz skyr."),
                new Meal("Schabowy", List.of(new Ingredient("schab", "150 g")), 800, 40, 50, 35, "Usmaż schab."),
                new Meal("Zupa", List.of(new Ingredient("warzywa", "300 g")), 600, 12, 40, 20,
                        "Gotuj warzywa do miękkości."))),
                List.of(new ShoppingItem(longName, "1 szt.")));

        assertThat(text(new PlanPdfWriter().dietPdf(plan, 2000))).contains("abcdefghijklmnopqrstuvwxyz");
        assertThat(text(new PlanPdfWriter().shoppingListPdf(plan))).contains("abcdefghijklmnopqrstuvwxyz");
    }

    @Test
    void shouldRenderPolishGlyphsAndShoppingItemsInTheShoppingListPdf() throws IOException {
        final String actual = text(new PlanPdfWriter().shoppingListPdf(samplePlan()));

        assertThat(actual).containsOnlyOnce("Lista zakupów");
        assertThat(actual).containsOnlyOnce("Żółć");
        assertThat(actual).containsOnlyOnce("jądro");
        assertThat(actual).containsOnlyOnce("chleb");
        assertThat(actual).contains("100 g", "60 g");
        assertThat(actual).doesNotContain("alice", "Dzień", "Jadłospis");
    }

    @Test
    void shouldRenderTheWeekShoppingListOnceWithoutADayOrADate() throws IOException {
        final String actual = text(new PlanPdfWriter().shoppingListPdf(shortWeek()));

        assertThat(actual).containsOnlyOnce("Lista zakupów");
        assertThat(actual).containsOnlyOnce("platki");
        assertThat(actual).containsOnlyOnce("350 g");
        assertThat(actual).containsOnlyOnce("jaja");
        assertThat(actual).containsOnlyOnce("14 szt.");
        assertThat(actual).doesNotContain(
                "Dzień",
                "Jadłospis",
                LocalDate.now(ZoneId.of("Europe/Warsaw")).plusDays(1)
                        .format(DateTimeFormatter.ofPattern("d.MM.yyyy")));
    }

    private String text(final byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private List<String> pagesOf(final byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return IntStream.rangeClosed(1, document.getNumberOfPages())
                    .mapToObj((final int page) -> textOf(document, page))
                    .toList();
        }
    }

    private String textOf(final PDDocument document, final int page) {
        try {
            final PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(page);
            stripper.setEndPage(page);
            return stripper.getText(document);
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private WeekPlan samplePlan() {
        return new WeekPlan(List.of(new DayMeals(
                new Meal("Żółć", List.of(new Ingredient("jądro", "100 g")), 400, 20, 10, 15, "Sparzyć żółć."),
                new Meal("Jogurt", List.of(new Ingredient("skyr", "200 g")), 200, 18, 8, 2, "Otwórz skyr."),
                new Meal("Schabowy", List.of(new Ingredient("schab", "150 g")), 800, 40, 50, 35, "Usmaż schab."),
                new Meal("Zupa", List.of(new Ingredient("warzywa", "300 g")), 600, 12, 40, 20,
                        "Gotuj warzywa do miękkości."))),
                List.of(
                        new ShoppingItem("Żółć", "100 g"),
                        new ShoppingItem("jądro", "100 g"),
                        new ShoppingItem("chleb", "60 g")));
    }

    private WeekPlan shortWeek() {
        return new WeekPlan(
                IntStream.rangeClosed(1, 7).mapToObj(this::shortDay).toList(),
                List.of(new ShoppingItem("platki", "350 g"), new ShoppingItem("jaja", "14 szt.")));
    }

    private WeekPlan weekWithALongFirstDay() {
        return new WeekPlan(
                IntStream.rangeClosed(1, 7)
                        .mapToObj((final int day) -> day == 1 ? longFirstDay() : shortDay(day))
                        .toList(),
                List.of(new ShoppingItem("platki", "350 g")));
    }

    private DayMeals longFirstDay() {
        final DayMeals day = shortDay(1);
        final Meal dinner = day.dinner();
        return new DayMeals(
                day.breakfast(),
                day.secondBreakfast(),
                day.lunch(),
                new Meal(
                        dinner.name(),
                        dinner.ingredients(),
                        dinner.kcal(),
                        dinner.proteinG(),
                        dinner.carbsG(),
                        dinner.fatG(),
                        "przygotowanie ".repeat(240) + "koniec-dnia"));
    }

    private WeekPlan detailedWeek() {
        return new WeekPlan(
                IntStream.rangeClosed(1, 7).mapToObj(this::detailedDay).toList(),
                List.of(new ShoppingItem("schab", "1 kg")));
    }

    private DayMeals detailedDay(final int day) {
        final String preparation = "Rozgrzej piekarnik do 180 °C. Piecz 25 minut w naczyniu żaroodpornym, "
                + "aż złapie rumieniec. Odstaw na 5 minut.";
        return new DayMeals(
                meal("Schab " + day, preparation),
                new Meal("Jogurt " + day, List.of(new Ingredient("skyr", "200 g")), 200, 18, 20, 4,
                        "Przełóż do miski i podaj od razu."),
                meal("Ryba " + day, preparation),
                meal("Kasza " + day, preparation));
    }

    private Meal meal(final String name, final String preparation) {
        return new Meal(name, List.of(
                new Ingredient("schab", "150 g"),
                new Ingredient("oliwa", "1 łyżka"),
                new Ingredient("sól", "szczypta"),
                new Ingredient("pieprz", "szczypta")),
                500, 30, 20, 15, preparation);
    }

    private DayMeals shortDay(final int day) {
        return new DayMeals(
                new Meal("Owsianka " + day, List.of(new Ingredient("platki", "50 g")), 400, 20, 40, 10, "Zagotuj."),
                new Meal("Jogurt " + day, List.of(new Ingredient("skyr", "200 g")), 200, 18, 20, 4, "Otwórz."),
                new Meal("Schab " + day, List.of(new Ingredient("schab", "150 g")), 800, 40, 30, 35, "Usmaż."),
                new Meal("Zupa " + day, List.of(new Ingredient("warzywa", "300 g")), 600, 12, 40, 18, "Gotuj."));
    }
}
