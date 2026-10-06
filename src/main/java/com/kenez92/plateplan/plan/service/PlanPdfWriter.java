package com.kenez92.plateplan.plan.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.kenez92.plateplan.plan.model.DietPlan;
import com.kenez92.plateplan.plan.model.Meal;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;

/**
 * The only place that turns a {@link DietPlan} into PDF bytes. Both files stay in memory. An
 * {@link IOException} propagates so a later layer can map it to unavailable. The login is never a
 * parameter and never appears in the text.
 */
@Service
public class PlanPdfWriter {

    private static final String FONT_RESOURCE = "/fonts/LiberationSans-Regular.ttf";
    private static final String FONT_MISSING = "Embedded font is missing: " + FONT_RESOURCE;
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d.MM.yyyy");
    private static final String DIET_TITLE = "Jadłospis na %s";
    private static final String DAILY_TARGET = "Cel dnia: %d kcal";
    private static final String SHOPPING_TITLE = "Lista zakupów";
    private static final String BREAKFAST = "Śniadanie";
    private static final String SECOND_BREAKFAST = "Drugie śniadanie";
    private static final String LUNCH = "Obiad";
    private static final String DINNER = "Kolacja";
    private static final String MEAL_HEADING = "%s — %s (%d kcal)";
    private static final String ITEM_PREFIX = "- ";
    private static final String BLANK_LINE = "";
    private static final float MARGIN = 50f;
    private static final float FONT_SIZE = 12f;
    private static final float LINE_HEIGHT = 16f;
    private static final int LINES_PER_PAGE =
            (int) ((PDRectangle.A4.getHeight() - (2 * MARGIN)) / LINE_HEIGHT);

    public byte[] dietPdf(final DietPlan dietPlan, final int dailyCalories) throws IOException {
        return write(dietLines(dietPlan, dailyCalories));
    }

    public byte[] shoppingListPdf(final DietPlan dietPlan) throws IOException {
        return write(shoppingLines(dietPlan));
    }

    private List<String> dietLines(final DietPlan dietPlan, final int dailyCalories) {
        final List<String> lines = new ArrayList<>();
        lines.add(DIET_TITLE.formatted(LocalDate.now(WARSAW).plusDays(1).format(DATE_FORMAT)));
        lines.add(DAILY_TARGET.formatted(dailyCalories));
        lines.add(BLANK_LINE);
        addMeal(lines, BREAKFAST, dietPlan.breakfast());
        addMeal(lines, SECOND_BREAKFAST, dietPlan.secondBreakfast());
        addMeal(lines, LUNCH, dietPlan.lunch());
        addMeal(lines, DINNER, dietPlan.dinner());
        return lines;
    }

    private void addMeal(final List<String> lines, final String heading, final Meal meal) {
        lines.add(MEAL_HEADING.formatted(heading, meal.name(), meal.kcal()));
        for (final String ingredient : meal.ingredients()) {
            lines.add(ITEM_PREFIX + ingredient);
        }
        lines.add(BLANK_LINE);
    }

    private List<String> shoppingLines(final DietPlan dietPlan) {
        final List<String> lines = new ArrayList<>();
        lines.add(SHOPPING_TITLE);
        for (final String item : dietPlan.shoppingList()) {
            lines.add(ITEM_PREFIX + item);
        }
        return lines;
    }

    private byte[] write(final List<String> lines) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            final PDType0Font font = loadFont(document);
            for (int start = 0; start < lines.size(); start += LINES_PER_PAGE) {
                writePage(document, font, lines.subList(start, Math.min(start + LINES_PER_PAGE, lines.size())));
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private void writePage(final PDDocument document,
                           final PDType0Font font,
                           final List<String> pageLines) throws IOException {
        final PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            float y = page.getMediaBox().getHeight() - MARGIN;
            for (final String line : pageLines) {
                content.beginText();
                content.setFont(font, FONT_SIZE);
                content.newLineAtOffset(MARGIN, y);
                content.showText(line);
                content.endText();
                y -= LINE_HEIGHT;
            }
        }
    }

    private PDType0Font loadFont(final PDDocument document) throws IOException {
        try (InputStream fontStream = PlanPdfWriter.class.getResourceAsStream(FONT_RESOURCE)) {
            if (fontStream == null) {
                throw new IOException(FONT_MISSING);
            }
            return PDType0Font.load(document, fontStream);
        }
    }
}
