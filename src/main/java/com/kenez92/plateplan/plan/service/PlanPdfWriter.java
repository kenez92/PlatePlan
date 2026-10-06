package com.kenez92.plateplan.plan.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.kenez92.plateplan.plan.model.DietPlan;
import com.kenez92.plateplan.plan.model.Ingredient;
import com.kenez92.plateplan.plan.model.Meal;
import com.kenez92.plateplan.plan.model.ShoppingItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
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
    private static final String WARSAW_ZONE = "Europe/Warsaw";
    private static final ZoneId WARSAW = ZoneId.of(WARSAW_ZONE);
    private static final String DATE_PATTERN = "d.MM.yyyy";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern(DATE_PATTERN);
    private static final String DIET_TITLE = "Jadłospis na %s";
    private static final String DAILY_TARGET = "Cel dnia: %d kcal";
    private static final String DAILY_MACROS = "Białko %d g  ·  Węglowodany %d g  ·  Tłuszcz %d g";
    private static final String MEAL_STATS = "%d kcal  ·  Białko %d g  ·  Węglowodany %d g  ·  Tłuszcz %d g";
    private static final String SHOPPING_TITLE = "Lista zakupów";
    private static final String BREAKFAST = "Śniadanie";
    private static final String SECOND_BREAKFAST = "Drugie śniadanie";
    private static final String LUNCH = "Obiad";
    private static final String DINNER = "Kolacja";
    private static final String INGREDIENTS_HEADING = "Składniki";
    private static final String PREPARATION_HEADING = "Przygotowanie";
    private static final String ITEM_PREFIX = "• ";
    private static final String NAMED_AMOUNT = "%s — %s";
    private static final String BLANK = "";
    private static final char CARRIAGE_RETURN = '\r';
    private static final char LINE_FEED = '\n';
    private static final char TAB = '\t';
    private static final char SPACE = ' ';
    private static final long TOMORROW_OFFSET = 1L;
    private static final int MARGIN_SIDES = 2;
    private static final int MIN_WRAP_CHARS = 1;
    private static final int NO_WORD_BREAK = 0;
    private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
    private static final float MARGIN = 48f;
    private static final float FONT_UNIT = 1000f;
    private static final float TITLE_SIZE = 20f;
    private static final float TITLE_HEIGHT = 28f;
    private static final float SLOT_SIZE = 10f;
    private static final float SLOT_HEIGHT = 16f;
    private static final float DISH_SIZE = 14f;
    private static final float DISH_HEIGHT = 20f;
    private static final float SECTION_SIZE = 11f;
    private static final float SECTION_HEIGHT = 18f;
    private static final float BODY_SIZE = 11f;
    private static final float BODY_HEIGHT = 16f;
    private static final float MUTED_SIZE = 10f;
    private static final float MUTED_HEIGHT = 15f;
    private static final float RULE_HEIGHT = 14f;
    private static final float SPACER_HEIGHT = 12f;
    private static final float BOLD_STROKE = 0.35f;
    private static final float RULE_STROKE = 0.8f;
    private static final float MAX_LINE_WIDTH = PAGE_SIZE.getWidth() - (MARGIN_SIDES * MARGIN);
    private static final float PAGE_BODY_HEIGHT = PAGE_SIZE.getHeight() - (MARGIN_SIDES * MARGIN);
    private static final PDColor TITLE_COLOR = rgb(0.12f, 0.28f, 0.22f);
    private static final PDColor SLOT_COLOR = rgb(0.36f, 0.45f, 0.40f);
    private static final PDColor BODY_COLOR = rgb(0.16f, 0.16f, 0.16f);
    private static final PDColor MUTED_COLOR = rgb(0.38f, 0.40f, 0.38f);

    public byte[] dietPdf(final DietPlan dietPlan, final int dailyCalories) throws IOException {
        return write(dietLines(dietPlan, dailyCalories));
    }

    public byte[] shoppingListPdf(final DietPlan dietPlan) throws IOException {
        return write(shoppingLines(dietPlan));
    }

    private List<PdfLine> dietLines(final DietPlan dietPlan, final int dailyCalories) {
        return Stream.of(
                        Stream.of(
                                new PdfLine(Role.TITLE, DIET_TITLE.formatted(tomorrow())),
                                new PdfLine(Role.MUTED, DAILY_TARGET.formatted(dailyCalories)),
                                new PdfLine(Role.MUTED, dailyMacros(dietPlan)),
                                new PdfLine(Role.RULE, BLANK)),
                        mealLines(BREAKFAST, dietPlan.breakfast()),
                        mealLines(SECOND_BREAKFAST, dietPlan.secondBreakfast()),
                        mealLines(LUNCH, dietPlan.lunch()),
                        mealLines(DINNER, dietPlan.dinner()))
                .flatMap(lines -> lines)
                .toList();
    }

    private String dailyMacros(final DietPlan dietPlan) {
        return DAILY_MACROS.formatted(
                total(dietPlan, Meal::proteinG),
                total(dietPlan, Meal::carbsG),
                total(dietPlan, Meal::fatG));
    }

    private int total(final DietPlan dietPlan, final ToIntFunction<Meal> grams) {
        return Stream.of(dietPlan.breakfast(), dietPlan.secondBreakfast(), dietPlan.lunch(), dietPlan.dinner())
                .mapToInt(grams)
                .sum();
    }

    private String tomorrow() {
        return LocalDate.now(WARSAW).plusDays(TOMORROW_OFFSET).format(DATE_FORMAT);
    }

    private Stream<PdfLine> mealLines(final String slot, final Meal meal) {
        return Stream.of(
                        Stream.of(
                                new PdfLine(Role.SLOT, slot),
                                new PdfLine(Role.DISH, meal.name()),
                                new PdfLine(Role.MUTED, MEAL_STATS.formatted(
                                        meal.kcal(), meal.proteinG(), meal.carbsG(), meal.fatG())),
                                new PdfLine(Role.SECTION, INGREDIENTS_HEADING)),
                        meal.ingredients().stream().map(this::ingredientLine),
                        preparationLines(meal),
                        Stream.of(new PdfLine(Role.SPACER, BLANK)))
                .flatMap(lines -> lines);
    }

    private Stream<PdfLine> preparationLines(final Meal meal) {
        if (meal.preparation().isBlank()) {
            return Stream.empty();
        }
        return Stream.of(
                new PdfLine(Role.SECTION, PREPARATION_HEADING),
                new PdfLine(Role.BODY, meal.preparation()));
    }

    private List<PdfLine> shoppingLines(final DietPlan dietPlan) {
        return Stream.concat(
                Stream.of(new PdfLine(Role.TITLE, SHOPPING_TITLE), new PdfLine(Role.RULE, BLANK)),
                dietPlan.shoppingList().stream().map(this::shoppingLine))
                .toList();
    }

    private PdfLine ingredientLine(final Ingredient ingredient) {
        return new PdfLine(Role.BODY, bullet(ingredient.name(), ingredient.amount()));
    }

    private PdfLine shoppingLine(final ShoppingItem item) {
        return new PdfLine(Role.BODY, bullet(item.name(), item.amount()));
    }

    private String bullet(final String name, final String amount) {
        if (amount.isBlank()) {
            return ITEM_PREFIX + name;
        }
        return ITEM_PREFIX + NAMED_AMOUNT.formatted(name, amount);
    }

    private byte[] write(final List<PdfLine> lines) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            fill(document, lines);
            document.save(output);
            return output.toByteArray();
        } catch (final UncheckedIOException exception) {
            throw exception.getCause();
        }
    }

    private void fill(final PDDocument document, final List<PdfLine> lines) throws IOException {
        final PDType0Font font = loadFont(document);
        pages(wrapAll(font, lines)).forEach(pageLines -> writePage(document, font, pageLines));
    }

    private List<List<PdfLine>> pages(final List<PdfLine> lines) {
        return lines.stream()
                .reduce(PageAcc.empty(), PageAcc::add, PageAcc::sequential)
                .finish();
    }

    private List<PdfLine> wrapAll(final PDType0Font font, final List<PdfLine> lines) {
        return lines.stream()
                .flatMap(line -> wrap(font, line).stream())
                .toList();
    }

    private List<PdfLine> wrap(final PDType0Font font, final PdfLine line) {
        if (!line.role().wrapped() || line.text().isEmpty()) {
            return List.of(line);
        }
        return wrapLine(font, line.role(), sanitize(line.text())).stream()
                .map(text -> new PdfLine(line.role(), text))
                .toList();
    }

    private List<String> wrapLine(final PDType0Font font, final Role role, final String line) {
        return Stream.iterate(
                        breakLine(font, role, line),
                        chunk -> chunk != null,
                        chunk -> chunk.rest().isEmpty() ? null : breakLine(font, role, chunk.rest()))
                .map(LineBreak::taken)
                .toList();
    }

    private LineBreak breakLine(final PDType0Font font, final Role role, final String remaining) {
        final int end = breakAt(font, role, remaining);
        return new LineBreak(remaining.substring(0, end).stripTrailing(), remaining.substring(end).stripLeading());
    }

    private int breakAt(final PDType0Font font, final Role role, final String remaining) {
        final int fit = fittingLength(font, role, remaining);
        if (fit >= remaining.length()) {
            return remaining.length();
        }
        final int space = remaining.lastIndexOf(SPACE, fit);
        return space > NO_WORD_BREAK ? space : fit;
    }

    private int fittingLength(final PDType0Font font, final Role role, final String remaining) {
        return IntStream.iterate(remaining.length(), fit -> fit > MIN_WRAP_CHARS, fit -> fit - 1)
                .filter(fit -> width(font, role, remaining.substring(0, fit)) <= MAX_LINE_WIDTH)
                .findFirst()
                .orElse(MIN_WRAP_CHARS);
    }

    private float width(final PDType0Font font, final Role role, final String text) {
        try {
            return font.getStringWidth(text) / FONT_UNIT * role.size();
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private String sanitize(final String line) {
        return line.replace(CARRIAGE_RETURN, SPACE).replace(LINE_FEED, SPACE).replace(TAB, SPACE);
    }

    private void writePage(final PDDocument document,
                           final PDType0Font font,
                           final List<PdfLine> pageLines) {
        try {
            final PDPage page = new PDPage(PAGE_SIZE);
            document.addPage(page);
            writeLines(document, page, font, pageLines);
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void writeLines(final PDDocument document,
                            final PDPage page,
                            final PDType0Font font,
                            final List<PdfLine> pageLines) throws IOException {
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            final List<Float> baselines = baselines(page, pageLines);
            IntStream.range(0, pageLines.size())
                    .forEach(index -> show(content, font, pageLines.get(index), baselines.get(index)));
        }
    }

    private List<Float> baselines(final PDPage page, final List<PdfLine> pageLines) {
        return pageLines.stream()
                .reduce(new YWalk(List.of(), page.getMediaBox().getHeight() - MARGIN),
                        YWalk::next,
                        YWalk::sequential)
                .ys();
    }

    private void show(final PDPageContentStream content,
                      final PDType0Font font,
                      final PdfLine line,
                      final float y) {
        try {
            if (line.role() == Role.RULE) {
                strokeRule(content, y);
                return;
            }
            if (line.role() == Role.SPACER || line.text().isEmpty()) {
                return;
            }
            content.setNonStrokingColor(line.role().color());
            content.setStrokingColor(line.role().color());
            content.setRenderingMode(line.role().bold() ? RenderingMode.FILL_STROKE : RenderingMode.FILL);
            content.setLineWidth(BOLD_STROKE);
            content.beginText();
            content.setFont(font, line.role().size());
            content.newLineAtOffset(MARGIN, y);
            content.showText(line.text());
            content.endText();
            content.setRenderingMode(RenderingMode.FILL);
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void strokeRule(final PDPageContentStream content, final float y) throws IOException {
        content.setStrokingColor(MUTED_COLOR);
        content.setLineWidth(RULE_STROKE);
        content.moveTo(MARGIN, y);
        content.lineTo(PAGE_SIZE.getWidth() - MARGIN, y);
        content.stroke();
    }

    private PDType0Font loadFont(final PDDocument document) throws IOException {
        try (InputStream fontStream = PlanPdfWriter.class.getResourceAsStream(FONT_RESOURCE)) {
            if (fontStream == null) {
                throw new IOException(FONT_MISSING);
            }
            return PDType0Font.load(document, fontStream);
        }
    }

    private static PDColor rgb(final float red, final float green, final float blue) {
        return new PDColor(new float[] {red, green, blue}, PDDeviceRGB.INSTANCE);
    }

    private enum Role {
        TITLE(TITLE_SIZE, TITLE_HEIGHT, true, TITLE_COLOR, true),
        SLOT(SLOT_SIZE, SLOT_HEIGHT, true, SLOT_COLOR, false),
        DISH(DISH_SIZE, DISH_HEIGHT, true, TITLE_COLOR, true),
        SECTION(SECTION_SIZE, SECTION_HEIGHT, true, TITLE_COLOR, false),
        BODY(BODY_SIZE, BODY_HEIGHT, false, BODY_COLOR, true),
        MUTED(MUTED_SIZE, MUTED_HEIGHT, false, MUTED_COLOR, true),
        RULE(0f, RULE_HEIGHT, false, MUTED_COLOR, false),
        SPACER(0f, SPACER_HEIGHT, false, BODY_COLOR, false);

        private final float size;
        private final float height;
        private final boolean bold;
        private final PDColor color;
        private final boolean wrapped;

        Role(final float size,
             final float height,
             final boolean bold,
             final PDColor color,
             final boolean wrapped) {
            this.size = size;
            this.height = height;
            this.bold = bold;
            this.color = color;
            this.wrapped = wrapped;
        }

        private float size() {
            return size;
        }

        private float height() {
            return height;
        }

        private boolean bold() {
            return bold;
        }

        private PDColor color() {
            return color;
        }

        private boolean wrapped() {
            return wrapped;
        }
    }

    private record PdfLine(Role role, String text) {
    }

    private record LineBreak(String taken, String rest) {
    }

    private record YWalk(List<Float> ys, float y) {

        private YWalk next(final PdfLine line) {
            return new YWalk(Stream.concat(ys.stream(), Stream.of(y)).toList(), y - line.role().height());
        }

        private static YWalk sequential(final YWalk left, final YWalk ignored) {
            return left;
        }
    }

    private record PageAcc(List<List<PdfLine>> done, List<PdfLine> current, float remaining) {

        private static PageAcc empty() {
            return new PageAcc(List.of(), List.of(), PAGE_BODY_HEIGHT);
        }

        private PageAcc add(final PdfLine line) {
            if (line.role() == Role.SPACER && current.isEmpty()) {
                return this;
            }
            if (line.role().height() > remaining && !current.isEmpty()) {
                return new PageAcc(
                        Stream.concat(done.stream(), Stream.of(current)).toList(),
                        List.of(line),
                        PAGE_BODY_HEIGHT - line.role().height());
            }
            return new PageAcc(
                    done,
                    Stream.concat(current.stream(), Stream.of(line)).toList(),
                    remaining - line.role().height());
        }

        private List<List<PdfLine>> finish() {
            if (current.isEmpty()) {
                return done;
            }
            return Stream.concat(done.stream(), Stream.of(current)).toList();
        }

        private static PageAcc sequential(final PageAcc left, final PageAcc ignored) {
            return left;
        }
    }
}
