package com.kenez92.plateplan.plan.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.kenez92.plateplan.plan.model.DayMeals;
import com.kenez92.plateplan.plan.model.Ingredient;
import com.kenez92.plateplan.plan.model.Meal;
import com.kenez92.plateplan.plan.model.PlanPerson;
import com.kenez92.plateplan.plan.model.Portion;
import com.kenez92.plateplan.plan.model.ShoppingItem;
import com.kenez92.plateplan.plan.model.WeekPlan;
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
 * The only place that turns a {@link WeekPlan} into PDF bytes. Both files stay in memory. An
 * {@link IOException} propagates so a later layer can map it to unavailable. The login is never a
 * parameter and never appears in the text. Bytes are not logged.
 */
@Service
public class PlanPdfWriter {

    private static final String FONT_RESOURCE = "/fonts/LiberationSans-Regular.ttf";
    private static final String FONT_MISSING = "Embedded font is missing: " + FONT_RESOURCE;
    private static final String DAY_TITLE = "Dzień %d";
    private static final String DAILY_TARGET = "Cel dnia — %s: %d kcal";
    private static final String DAILY_MACROS = "Białko %d g  ·  Węglowodany %d g  ·  Tłuszcz %d g";
    private static final String PORTION_STATS = "%s  ·  %d kcal  ·  Białko %d g  ·  Węglowodany %d g  ·  Tłuszcz %d g";
    private static final String SHOPPING_TITLE = "Lista zakupów";
    private static final String VEGETABLES = "Warzywa i owoce";
    private static final String BREAD = "Pieczywo";
    private static final String DAIRY = "Nabiał";
    private static final String MEAT = "Mięso";
    private static final String COLD_CUTS = "Wędliny";
    private static final String FISH = "Ryby";
    private static final String GRAINS = "Produkty zbożowe";
    private static final String FATS = "Tłuszcze";
    private static final String SPICES = "Przyprawy";
    private static final String OTHER = "Inne";
    private static final String WORD_BREAK = " ";
    private static final String TOKEN_SPLIT = "[^\\p{L}\\p{N}]+";
    private static final List<String> NOUN_TAILS = List.of(
            "a", "y", "i", "u", "e", "ę", "ą", "em", "om", "ów", "ami", "ach", "ie", "ł", "ły", "ka", "ki", "kę");
    private static final List<String> CATEGORIES = List.of(
            VEGETABLES, BREAD, DAIRY, MEAT, COLD_CUTS, FISH, GRAINS, FATS, SPICES, OTHER);
    private static final List<Shelf> SHELVES = List.of(
            new Shelf(SPICES, List.of(
                    "sól", "pieprz", "papryka słodka", "papryka ostra", "papryka w proszku", "zioł", "bazylia",
                    "oregano", "tymianek", "rozmaryn", "curry", "kurkuma", "cynamon", "kminek", "chili",
                    "gałka", "laurow", "majeranek", "kolendra", "imbir", "musztard", "ocet")),
            new Shelf(COLD_CUTS, List.of(
                    "szynka", "kiełbas", "kabanos", "parów", "salami", "mortadel", "pasztet", "wędlin",
                    "boczek", "baleron", "kindziuk", "sopocka")),
            new Shelf(FISH, List.of(
                    "łosoś", "losos", "dorsz", "mintaj", "tuńczyk", "tunczyk", "pstrąg", "pstrag", "krewet",
                    "makrel", "śledź", "sledz", "sandacz", "halibut", "morszczuk", "owoce morza", "ryba", "ryby")),
            new Shelf(GRAINS, List.of(
                    "ryż", "ryz", "kasza", "kasz", "makaron", "mąka", "maka", "płatki", "platki", "owsian",
                    "bułka tarta")),
            new Shelf(FATS, List.of("oliwa", "olej", "masło", "maslo", "margaryn", "smalec")),
            new Shelf(BREAD, List.of("chleb", "bułk", "bulka", "bagiet", "pita", "tortilla", "kajzer", "precel", "pieczyw")),
            new Shelf(DAIRY, List.of(
                    "mleko", "jogurt", "skyr", "twaróg", "twarog", "śmietan", "smietan", "kefir", "maślank",
                    "maslank", "feta", "mozzarella", "ricotta", "jaj", "ser")),
            new Shelf(MEAT, List.of(
                    "kurczak", "indyk", "wołow", "wieprz", "schab", "karków", "udko", "udziec", "filet",
                    "mielon", "wątrob", "watrob", "polędwic", "poledwic", "mięso", "mieso")),
            new Shelf(VEGETABLES, List.of(
                    "pomidor", "ogórek", "ogorek", "sałat", "salat", "broku", "marchew", "ziemniak", "jabł",
                    "jabl", "banan", "cytryn", "limonk", "awokado", "cukini", "bakła", "bakla", "papryka",
                    "szpinak", "kalafior", "kapust", "cebul", "czosnek", "pietrusz", "seler", "burak", "dyni",
                    "grusz", "śliw", "sliw", "truskaw", "malin", "borów", "borow", "winogron", "kiwi", "ananas",
                    "arbuz", "melon", "pieczark", "cukinia", "por")));
    private static final String BREAKFAST = "Śniadanie";
    private static final String SECOND_BREAKFAST = "Drugie śniadanie";
    private static final String LUNCH = "Obiad";
    private static final String DINNER = "Kolacja";
    private static final String INGREDIENTS_HEADING = "Składniki";
    private static final String TOTAL_HEADING = "Łącznie";
    private static final String SHARE_LABEL = "Udział z garnka";
    private static final String SHARE_CELL = "%d%%";
    private static final String FULL_POT = "100%";
    private static final String MISSING_AMOUNT = "—";
    private static final String AMOUNT_JOINER = ", ";
    private static final String NUMBER_AND_UNIT = "%s %s";
    private static final String REPEATED_AMOUNT = "%d× %s";
    private static final String PREPARATION_HEADING = "Przygotowanie";
    private static final String ITEM_PREFIX = "• ";
    private static final String NAMED_AMOUNT = "%s — %s";
    private static final String BLANK = "";
    private static final char DECIMAL_COMMA = ',';
    private static final char DECIMAL_POINT = '.';
    private static final char CARRIAGE_RETURN = '\r';
    private static final char LINE_FEED = '\n';
    private static final char TAB = '\t';
    private static final char SPACE = ' ';
    private static final int FIRST_DAY = 0;
    private static final int DAY_NUMBER_OFFSET = 1;
    private static final int PERCENT = 100;
    private static final int NO_KCAL = 0;
    private static final int NO_SHARE = 0;
    private static final int ONE_SHARE = 1;
    private static final int SINGLE_AMOUNT = 1;
    private static final int FIRST_CHARACTER = 0;
    private static final int DIGIT_ZERO = '0';
    private static final int DIGIT_NINE = '9';
    private static final int NAME_AND_TOTAL = 2;
    private static final int SHOPPING_COLUMNS = 2;
    private static final int STEM = 4;
    private static final int MARGIN_SIDES = 2;
    private static final int MIN_WRAP_CHARS = 1;
    private static final int NO_WORD_BREAK = 0;
    private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
    private static final float MARGIN = 48f;
    private static final float FONT_UNIT = 1000f;
    private static final float TITLE_SIZE = 16f;
    private static final float TITLE_HEIGHT = 22f;
    private static final float SLOT_SIZE = 9f;
    private static final float SLOT_HEIGHT = 13f;
    private static final float DISH_SIZE = 13f;
    private static final float DISH_HEIGHT = 17f;
    private static final float SECTION_SIZE = 10f;
    private static final float SECTION_HEIGHT = 14f;
    private static final float BODY_SIZE = 10f;
    private static final float BODY_HEIGHT = 13f;
    private static final float MUTED_SIZE = 9f;
    private static final float MUTED_HEIGHT = 12f;
    private static final float TABLE_SIZE = 9f;
    private static final float TABLE_HEIGHT = 16f;
    private static final float RULE_HEIGHT = 8f;
    private static final float SPACER_HEIGHT = 6f;
    private static final float BOLD_STROKE = 0.35f;
    private static final float RULE_STROKE = 0.8f;
    private static final float TABLE_RULE_STROKE = 0.4f;
    private static final float NAME_COLUMN = 188f;
    private static final float COLUMN_PAD = 8f;
    private static final float SHOPPING_GAP = 18f;
    private static final float CELL_INSET = 6f;
    private static final float TABLE_ABOVE_BASELINE = 8f;
    private static final float TABLE_RULE_BELOW_BASELINE = 5f;
    private static final float MIN_CELL_WIDTH = 1f;
    private static final float ZERO_HEIGHT = 0f;
    private static final float MAX_LINE_WIDTH = PAGE_SIZE.getWidth() - (MARGIN_SIDES * MARGIN);
    private static final float PAGE_BODY_HEIGHT = PAGE_SIZE.getHeight() - (MARGIN_SIDES * MARGIN);
    private static final PDColor TITLE_COLOR = rgb(0.12f, 0.28f, 0.22f);
    private static final PDColor SLOT_COLOR = rgb(0.36f, 0.45f, 0.40f);
    private static final PDColor BODY_COLOR = rgb(0.16f, 0.16f, 0.16f);
    private static final PDColor MUTED_COLOR = rgb(0.38f, 0.40f, 0.38f);

    public byte[] dietPdf(final WeekPlan weekPlan, final List<PlanPerson> people) throws IOException {
        return write(dietLines(weekPlan, people));
    }

    public byte[] shoppingListPdf(final WeekPlan weekPlan) throws IOException {
        return write(shoppingLines(weekPlan));
    }

    private List<PdfLine> dietLines(final WeekPlan weekPlan, final List<PlanPerson> people) {
        return IntStream.range(0, weekPlan.days().size())
                .mapToObj((final int index) -> dayLines(weekPlan.days().get(index), index, people))
                .flatMap((final Stream<PdfLine> lines) -> lines)
                .toList();
    }

    private Stream<PdfLine> dayLines(final DayMeals day, final int index, final List<PlanPerson> people) {
        return Stream.of(
                        pageBreak(index),
                        Stream.of(new PdfLine(Role.TITLE, dayTitle(index))),
                        targetLines(day, people),
                        Stream.of(new PdfLine(Role.RULE, BLANK)),
                        mealLines(BREAKFAST, day.breakfast(), people),
                        mealLines(SECOND_BREAKFAST, day.secondBreakfast(), people),
                        mealLines(LUNCH, day.lunch(), people),
                        mealLines(DINNER, day.dinner(), people))
                .flatMap((final Stream<PdfLine> lines) -> lines);
    }

    private Stream<PdfLine> targetLines(final DayMeals day, final List<PlanPerson> people) {
        return people.stream().flatMap((final PlanPerson person) -> Stream.of(
                new PdfLine(Role.MUTED, DAILY_TARGET.formatted(person.name(), person.kcal())),
                new PdfLine(Role.MUTED, personMacros(day, person.name()))));
    }

    private Stream<PdfLine> pageBreak(final int index) {
        if (index == FIRST_DAY) {
            return Stream.empty();
        }
        return Stream.of(new PdfLine(Role.PAGE_BREAK, BLANK));
    }

    private String dayTitle(final int index) {
        return DAY_TITLE.formatted(index + DAY_NUMBER_OFFSET);
    }

    private String personMacros(final DayMeals day, final String personName) {
        return DAILY_MACROS.formatted(
                personSum(day, personName, Portion::proteinG),
                personSum(day, personName, Portion::carbsG),
                personSum(day, personName, Portion::fatG));
    }

    private int personSum(final DayMeals day, final String personName, final ToIntFunction<Portion> value) {
        return Stream.of(day.breakfast(), day.secondBreakfast(), day.lunch(), day.dinner())
                .flatMap((final Meal meal) -> meal.portions().stream())
                .filter((final Portion portion) -> sameName(personName, portion.personName()))
                .mapToInt(value)
                .sum();
    }

    private Stream<PdfLine> mealLines(final String slot, final Meal meal, final List<PlanPerson> people) {
        return Stream.of(
                        Stream.of(new PdfLine(Role.SLOT, slot), new PdfLine(Role.DISH, meal.name())),
                        ingredientTable(meal, people),
                        people.stream().flatMap((final PlanPerson person) -> statsLine(meal, person.name())),
                        preparationLines(meal),
                        Stream.of(new PdfLine(Role.SPACER, BLANK)))
                .flatMap((final Stream<PdfLine> lines) -> lines);
    }

    private Stream<PdfLine> ingredientTable(final Meal meal, final List<PlanPerson> people) {
        final List<String> names = ingredientNames(meal, people);
        if (names.isEmpty() || people.isEmpty()) {
            return Stream.empty();
        }
        return Stream.concat(
                Stream.of(
                        tableRow(Role.TABLE_HEAD, headerCells(people)),
                        tableRow(Role.TABLE, shareCells(meal, people))),
                names.stream().map((final String name) -> tableRow(Role.TABLE, amountCells(meal, people, name))));
    }

    private List<String> headerCells(final List<PlanPerson> people) {
        return Stream.concat(
                Stream.concat(Stream.of(INGREDIENTS_HEADING), people.stream().map(PlanPerson::name)),
                Stream.of(TOTAL_HEADING)).toList();
    }

    private List<String> shareCells(final Meal meal, final List<PlanPerson> people) {
        final List<Integer> shares = shares(people.stream()
                .map((final PlanPerson person) -> portionKcal(meal, person.name()))
                .toList());
        return Stream.concat(
                Stream.concat(
                        Stream.of(SHARE_LABEL),
                        shares.stream().map((final Integer share) -> SHARE_CELL.formatted(share))),
                Stream.of(FULL_POT)).toList();
    }

    private List<String> amountCells(final Meal meal, final List<PlanPerson> people, final String ingredient) {
        return Stream.concat(
                Stream.concat(
                        Stream.of(ingredient),
                        people.stream().map((final PlanPerson person) -> amount(meal, person.name(), ingredient))),
                Stream.of(total(meal, people, ingredient))).toList();
    }

    private String total(final Meal meal, final List<PlanPerson> people, final String ingredient) {
        final List<String> amounts = people.stream()
                .map((final PlanPerson person) -> amount(meal, person.name(), ingredient))
                .filter((final String value) -> !value.equals(MISSING_AMOUNT))
                .toList();
        if (amounts.isEmpty()) {
            return MISSING_AMOUNT;
        }
        return sumAmounts(amounts);
    }

    private String sumAmounts(final List<String> amounts) {
        final List<Measured> measured = amounts.stream().map(this::measure).toList();
        final List<String> units = measured.stream()
                .map(Measured::unit)
                .reduce(List.of(), this::rememberName, (final List<String> left, final List<String> ignored) -> left);
        return String.join(AMOUNT_JOINER, units.stream().map((final String unit) -> sumUnit(measured, unit)).toList());
    }

    private String sumUnit(final List<Measured> measured, final String unit) {
        final List<Measured> same = measured.stream()
                .filter((final Measured item) -> sameName(item.unit(), unit))
                .toList();
        if (same.stream().anyMatch((final Measured item) -> item.number() == null)) {
            return repeated(same);
        }
        return formatAmount(same.stream().map(Measured::number).reduce(BigDecimal.ZERO, BigDecimal::add), unit);
    }

    private String repeated(final List<Measured> same) {
        final List<String> texts = same.stream()
                .map(Measured::text)
                .reduce(List.of(), this::rememberName, (final List<String> left, final List<String> ignored) -> left);
        if (texts.size() > SINGLE_AMOUNT) {
            return String.join(AMOUNT_JOINER, texts);
        }
        if (same.size() == SINGLE_AMOUNT) {
            return texts.getFirst();
        }
        return REPEATED_AMOUNT.formatted(same.size(), texts.getFirst());
    }

    private String formatAmount(final BigDecimal sum, final String unit) {
        final String number = sum.stripTrailingZeros().toPlainString();
        if (unit.isBlank()) {
            return number;
        }
        return NUMBER_AND_UNIT.formatted(number, unit);
    }

    private Measured measure(final String amount) {
        final String text = amount.strip();
        final int end = numberEnd(text);
        if (end == FIRST_CHARACTER) {
            return new Measured(null, text, text);
        }
        try {
            return new Measured(
                    new BigDecimal(text.substring(FIRST_CHARACTER, end).replace(DECIMAL_COMMA, DECIMAL_POINT)),
                    text.substring(end).strip(),
                    text);
        } catch (final NumberFormatException exception) {
            return new Measured(null, text, text);
        }
    }

    private int numberEnd(final String text) {
        return IntStream.range(FIRST_CHARACTER, text.length())
                .filter((final int index) -> !numberCharacter(text, index))
                .findFirst()
                .orElse(text.length());
    }

    private boolean numberCharacter(final String text, final int index) {
        final int character = text.charAt(index);
        if (character >= DIGIT_ZERO && character <= DIGIT_NINE) {
            return true;
        }
        if (character != DECIMAL_COMMA && character != DECIMAL_POINT) {
            return false;
        }
        return index > FIRST_CHARACTER
                && index + SINGLE_AMOUNT < text.length()
                && digit(text.charAt(index - SINGLE_AMOUNT))
                && digit(text.charAt(index + SINGLE_AMOUNT));
    }

    private boolean digit(final int character) {
        return character >= DIGIT_ZERO && character <= DIGIT_NINE;
    }

    private PdfLine tableRow(final Role role, final List<String> cells) {
        return new PdfLine(role, BLANK, cells.stream().map(List::of).toList());
    }

    private List<String> ingredientNames(final Meal meal, final List<PlanPerson> people) {
        return people.stream()
                .flatMap((final PlanPerson person) -> matched(meal, person.name()))
                .flatMap((final Portion portion) -> portion.ingredients().stream())
                .map(Ingredient::name)
                .filter((final String name) -> !name.isBlank())
                .reduce(List.of(), this::rememberName, (final List<String> left, final List<String> ignored) -> left);
    }

    private List<String> rememberName(final List<String> names, final String name) {
        if (names.stream().anyMatch((final String existing) -> sameName(existing, name))) {
            return names;
        }
        return Stream.concat(names.stream(), Stream.of(name)).toList();
    }

    private String amount(final Meal meal, final String personName, final String ingredient) {
        return matched(meal, personName)
                .flatMap((final Portion portion) -> portion.ingredients().stream())
                .filter((final Ingredient item) -> sameName(item.name(), ingredient))
                .map(Ingredient::amount)
                .filter((final String value) -> !value.isBlank())
                .findFirst()
                .orElse(MISSING_AMOUNT);
    }

    private Stream<PdfLine> statsLine(final Meal meal, final String personName) {
        return matched(meal, personName).map((final Portion portion) -> new PdfLine(Role.MUTED, PORTION_STATS.formatted(
                portion.personName(),
                portion.kcal(),
                portion.proteinG(),
                portion.carbsG(),
                portion.fatG())));
    }

    private int portionKcal(final Meal meal, final String personName) {
        return matched(meal, personName).mapToInt(Portion::kcal).findFirst().orElse(NO_KCAL);
    }

    private Stream<Portion> matched(final Meal meal, final String personName) {
        return meal.portions().stream()
                .filter((final Portion portion) -> sameName(personName, portion.personName()));
    }

    private List<Integer> shares(final List<Integer> kcal) {
        final int total = kcal.stream().mapToInt(Integer::intValue).sum();
        if (total <= NO_KCAL) {
            return evenShares(kcal.size());
        }
        final List<Integer> base = kcal.stream()
                .map((final Integer value) -> value * PERCENT / total)
                .toList();
        final int drift = PERCENT - base.stream().mapToInt(Integer::intValue).sum();
        final List<Integer> extra = IntStream.range(0, kcal.size())
                .boxed()
                .sorted(Comparator.comparingInt((final Integer index) -> Math.floorMod(kcal.get(index) * PERCENT, total))
                        .reversed()
                        .thenComparingInt((final Integer index) -> index))
                .limit(Math.max(drift, NO_SHARE))
                .toList();
        return IntStream.range(0, kcal.size())
                .map((final int index) -> base.get(index) + (extra.contains(index) ? ONE_SHARE : NO_SHARE))
                .boxed()
                .toList();
    }

    private List<Integer> evenShares(final int count) {
        if (count == NO_SHARE) {
            return List.of();
        }
        final int base = PERCENT / count;
        final int drift = PERCENT - (base * count);
        return IntStream.range(0, count)
                .map((final int index) -> base + (index < drift ? ONE_SHARE : NO_SHARE))
                .boxed()
                .toList();
    }

    private boolean sameName(final String left, final String right) {
        return matchKey(left).equals(matchKey(right));
    }

    private String matchKey(final String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }

    private Stream<PdfLine> preparationLines(final Meal meal) {
        if (meal.preparation().isBlank()) {
            return Stream.empty();
        }
        return Stream.of(
                new PdfLine(Role.SECTION, PREPARATION_HEADING),
                new PdfLine(Role.BODY, meal.preparation()));
    }

    private List<PdfLine> shoppingLines(final WeekPlan weekPlan) {
        return Stream.concat(
                Stream.of(new PdfLine(Role.TITLE, SHOPPING_TITLE), new PdfLine(Role.RULE, BLANK)),
                shelves(weekPlan.shoppingList()).stream().flatMap(this::shelfLines))
                .toList();
    }

    private List<List<ShoppingItem>> shelves(final List<ShoppingItem> items) {
        return CATEGORIES.stream()
                .map((final String category) -> items.stream()
                        .filter((final ShoppingItem item) -> shelf(item).equals(category))
                        .toList())
                .filter((final List<ShoppingItem> group) -> !group.isEmpty())
                .toList();
    }

    private Stream<PdfLine> shelfLines(final List<ShoppingItem> items) {
        return Stream.concat(
                Stream.of(new PdfLine(Role.CATEGORY, shelf(items.getFirst()))),
                Stream.concat(
                        IntStream.range(0, (items.size() + SINGLE_AMOUNT) / SHOPPING_COLUMNS)
                                .mapToObj((final int row) -> shoppingRow(items, row)),
                        Stream.of(new PdfLine(Role.SPACER, BLANK))));
    }

    private String shelf(final ShoppingItem item) {
        final String fromName = classify(matchKey(item.name()));
        if (!OTHER.equals(fromName)) {
            return fromName;
        }
        return knownCategory(item.category());
    }

    private String knownCategory(final String category) {
        final String given = matchKey(category);
        return CATEGORIES.stream()
                .filter((final String candidate) -> matchKey(candidate).equals(given))
                .findFirst()
                .orElse(OTHER);
    }

    private String classify(final String name) {
        return SHELVES.stream()
                .filter((final Shelf shelf) -> shelf.words().stream().anyMatch((final String word) -> mentions(name, word)))
                .map(Shelf::category)
                .findFirst()
                .orElse(OTHER);
    }

    private boolean mentions(final String name, final String word) {
        if (word.contains(WORD_BREAK)) {
            return name.contains(word);
        }
        return Stream.of(name.split(TOKEN_SPLIT))
                .filter((final String token) -> !token.isEmpty())
                .anyMatch((final String token) -> token.equals(word) || continued(token, word));
    }

    private boolean continued(final String token, final String word) {
        if (!token.startsWith(word) || token.length() == word.length()) {
            return false;
        }
        if (word.length() < STEM) {
            return true;
        }
        return NOUN_TAILS.contains(token.substring(word.length()));
    }

    private PdfLine shoppingRow(final List<ShoppingItem> items, final int row) {
        final int left = row * SHOPPING_COLUMNS;
        final int right = left + SINGLE_AMOUNT;
        return tableRow(Role.PAIR, List.of(
                bullet(items.get(left).name(), items.get(left).amount()),
                right < items.size() ? bullet(items.get(right).name(), items.get(right).amount()) : BLANK));
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
        return blocks(lines).stream()
                .reduce(PageAcc.empty(), PageAcc::addBlock, PageAcc::sequential)
                .finish();
    }

    private List<List<PdfLine>> blocks(final List<PdfLine> lines) {
        return lines.stream()
                .reduce(BlockAcc.empty(), BlockAcc::add, BlockAcc::sequential)
                .finish();
    }

    private List<PdfLine> wrapAll(final PDType0Font font, final List<PdfLine> lines) {
        return lines.stream()
                .flatMap(line -> wrap(font, line).stream())
                .toList();
    }

    private List<PdfLine> wrap(final PDType0Font font, final PdfLine line) {
        if (line.table()) {
            return List.of(wrapTable(font, line));
        }
        if (!line.role().wrapped() || line.text().isEmpty()) {
            return List.of(line);
        }
        return wrapLine(font, line.role(), sanitize(line.text()), MAX_LINE_WIDTH).stream()
                .map(text -> new PdfLine(line.role(), text))
                .toList();
    }

    private PdfLine wrapTable(final PDType0Font font, final PdfLine line) {
        final int columns = line.cells().size();
        return new PdfLine(line.role(), BLANK, IntStream.range(0, columns)
                .mapToObj((final int index) -> wrapCell(
                        font, line.role(), line.cells().get(index).getFirst(), cellWidth(line.role(), index, columns)))
                .toList());
    }

    private List<String> wrapCell(final PDType0Font font, final Role role, final String text, final float maxWidth) {
        final String clean = sanitize(text);
        if (clean.isEmpty()) {
            return List.of(BLANK);
        }
        return wrapLine(font, role, clean, maxWidth);
    }

    private List<String> wrapLine(final PDType0Font font, final Role role, final String line, final float maxWidth) {
        return Stream.iterate(
                        breakLine(font, role, line, maxWidth),
                        chunk -> chunk != null,
                        chunk -> chunk.rest().isEmpty() ? null : breakLine(font, role, chunk.rest(), maxWidth))
                .map(LineBreak::taken)
                .toList();
    }

    private LineBreak breakLine(final PDType0Font font, final Role role, final String remaining, final float maxWidth) {
        final int end = breakAt(font, role, remaining, maxWidth);
        return new LineBreak(remaining.substring(0, end).stripTrailing(), remaining.substring(end).stripLeading());
    }

    private int breakAt(final PDType0Font font, final Role role, final String remaining, final float maxWidth) {
        final int fit = fittingLength(font, role, remaining, maxWidth);
        if (fit >= remaining.length()) {
            return remaining.length();
        }
        final int space = remaining.lastIndexOf(SPACE, fit);
        return space > NO_WORD_BREAK ? space : fit;
    }

    private int fittingLength(final PDType0Font font, final Role role, final String remaining, final float maxWidth) {
        return IntStream.iterate(remaining.length(), fit -> fit > MIN_WRAP_CHARS, fit -> fit - 1)
                .filter(fit -> width(font, role, remaining.substring(0, fit)) <= maxWidth)
                .findFirst()
                .orElse(MIN_WRAP_CHARS);
    }

    private float cellWidth(final Role role, final int index, final int columns) {
        if (role == Role.PAIR) {
            return Math.max(pairSlot() - COLUMN_PAD, MIN_CELL_WIDTH);
        }
        final float width = index == 0
                ? NAME_COLUMN - COLUMN_PAD
                : personColumn(columns) - CELL_INSET - COLUMN_PAD;
        return Math.max(width, MIN_CELL_WIDTH);
    }

    private float pairSlot() {
        return (MAX_LINE_WIDTH - SHOPPING_GAP) / SHOPPING_COLUMNS;
    }

    private float personColumn(final int columns) {
        if (columns <= 1) {
            return MAX_LINE_WIDTH;
        }
        return (MAX_LINE_WIDTH - NAME_COLUMN) / (columns - 1);
    }

    private float cellX(final Role role, final int index, final int columns) {
        if (role == Role.PAIR) {
            return MARGIN + (index * (pairSlot() + SHOPPING_GAP));
        }
        if (index == 0) {
            return MARGIN;
        }
        return MARGIN + NAME_COLUMN + CELL_INSET + ((index - 1) * personColumn(columns));
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
            if (line.table()) {
                drawTable(content, font, line, y);
                return;
            }
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

    private void drawTable(final PDPageContentStream content,
                           final PDType0Font font,
                           final PdfLine line,
                           final float y) {
        try {
            content.setNonStrokingColor(line.role().color());
            content.setStrokingColor(line.role().color());
            content.setRenderingMode(line.role().bold() ? RenderingMode.FILL_STROKE : RenderingMode.FILL);
            content.setLineWidth(BOLD_STROKE);
            final int columns = line.cells().size();
            IntStream.range(0, columns).forEach((final int index) -> drawCell(
                    content, font, line, cellX(line.role(), index, columns), y, line.cells().get(index)));
            content.setRenderingMode(RenderingMode.FILL);
            if (line.role() != Role.PAIR) {
                strokeTableRules(content, line, y);
            }
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void drawCell(final PDPageContentStream content,
                          final PDType0Font font,
                          final PdfLine line,
                          final float x,
                          final float y,
                          final List<String> cell) {
        IntStream.range(0, cell.size())
                .forEach((final int row) -> drawText(
                        content, font, line, x, y - (row * line.role().height()), cell.get(row)));
    }

    private void drawText(final PDPageContentStream content,
                          final PDType0Font font,
                          final PdfLine line,
                          final float x,
                          final float y,
                          final String text) {
        if (text.isEmpty()) {
            return;
        }
        try {
            content.beginText();
            content.setFont(font, line.role().size());
            content.newLineAtOffset(x, y);
            content.showText(text);
            content.endText();
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void strokeTableRules(final PDPageContentStream content, final PdfLine line, final float y)
            throws IOException {
        final int rows = Math.max(line.cells().stream().mapToInt(List::size).max().orElse(SINGLE_AMOUNT), SINGLE_AMOUNT);
        final float ruleY = y - ((rows - 1) * line.role().height()) - TABLE_RULE_BELOW_BASELINE;
        content.setStrokingColor(MUTED_COLOR);
        content.setLineWidth(TABLE_RULE_STROKE);
        content.moveTo(MARGIN, ruleY);
        content.lineTo(MARGIN + MAX_LINE_WIDTH, ruleY);
        content.stroke();
        if (line.cells().size() <= SINGLE_AMOUNT) {
            return;
        }
        strokeVertical(content, line, MARGIN + NAME_COLUMN, y);
        if (line.cells().size() > NAME_AND_TOTAL) {
            strokeVertical(content, line, totalColumnX(line.cells().size()), y);
        }
    }

    private void strokeVertical(final PDPageContentStream content, final PdfLine line, final float x, final float y)
            throws IOException {
        content.moveTo(x, y + TABLE_ABOVE_BASELINE);
        content.lineTo(x, y - line.blockHeight() + TABLE_ABOVE_BASELINE);
        content.stroke();
    }

    private float totalColumnX(final int columns) {
        return MARGIN + NAME_COLUMN + ((columns - NAME_AND_TOTAL) * personColumn(columns));
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
        CATEGORY(SECTION_SIZE, SECTION_HEIGHT, true, TITLE_COLOR, true),
        BODY(BODY_SIZE, BODY_HEIGHT, false, BODY_COLOR, true),
        MUTED(MUTED_SIZE, MUTED_HEIGHT, false, MUTED_COLOR, true),
        TABLE_HEAD(TABLE_SIZE, TABLE_HEIGHT, true, TITLE_COLOR, false),
        TABLE(TABLE_SIZE, TABLE_HEIGHT, false, BODY_COLOR, false),
        PAIR(BODY_SIZE, BODY_HEIGHT, false, BODY_COLOR, false),
        RULE(0f, RULE_HEIGHT, false, MUTED_COLOR, false),
        SPACER(0f, SPACER_HEIGHT, false, BODY_COLOR, false),
        PAGE_BREAK(0f, 0f, false, BODY_COLOR, false);

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

    private record Measured(BigDecimal number, String unit, String text) {
    }

    private record Shelf(String category, List<String> words) {
    }

    private record PdfLine(Role role, String text, List<List<String>> cells) {

        private PdfLine(final Role role, final String text) {
            this(role, text, List.of());
        }

        private boolean table() {
            return role == Role.TABLE || role == Role.TABLE_HEAD || role == Role.PAIR;
        }

        private float blockHeight() {
            final int rows = cells.stream().mapToInt(List::size).max().orElse(0);
            if (rows == 0) {
                return role.height();
            }
            return rows * role.height();
        }
    }

    private record LineBreak(String taken, String rest) {
    }

    private record YWalk(List<Float> ys, float y) {

        private YWalk next(final PdfLine line) {
            return new YWalk(Stream.concat(ys.stream(), Stream.of(y)).toList(), y - line.blockHeight());
        }

        private static YWalk sequential(final YWalk left, final YWalk ignored) {
            return left;
        }
    }

    private record PageAcc(List<List<PdfLine>> done, List<PdfLine> current, float remaining) {

        private static PageAcc empty() {
            return new PageAcc(List.of(), List.of(), PAGE_BODY_HEIGHT);
        }

        private PageAcc addBlock(final List<PdfLine> block) {
            if (block.getFirst().role() == Role.PAGE_BREAK) {
                final List<PdfLine> rest = block.stream().skip(SINGLE_AMOUNT).toList();
                if (rest.isEmpty()) {
                    return closePage();
                }
                return closePage().place(rest);
            }
            return place(block);
        }

        private PageAcc place(final List<PdfLine> block) {
            final float height = block.stream().map(PdfLine::blockHeight).reduce(ZERO_HEIGHT, Float::sum);
            if (height > remaining && !current.isEmpty() && height <= PAGE_BODY_HEIGHT) {
                return closePage().place(block);
            }
            return block.stream().reduce(this, PageAcc::add, PageAcc::sequential);
        }

        private PageAcc add(final PdfLine line) {
            if (line.role() == Role.PAGE_BREAK) {
                return closePage();
            }
            if (line.role() == Role.SPACER && current.isEmpty()) {
                return this;
            }
            if (line.blockHeight() > remaining && !current.isEmpty()) {
                if (!drawsText(line)) {
                    return this;
                }
                return new PageAcc(
                        Stream.concat(done.stream(), Stream.of(current)).toList(),
                        List.of(line),
                        PAGE_BODY_HEIGHT - line.blockHeight());
            }
            return new PageAcc(
                    done,
                    Stream.concat(current.stream(), Stream.of(line)).toList(),
                    remaining - line.blockHeight());
        }

        private PageAcc closePage() {
            if (current.stream().noneMatch(this::drawsText)) {
                return new PageAcc(done, List.of(), PAGE_BODY_HEIGHT);
            }
            return new PageAcc(
                    Stream.concat(done.stream(), Stream.of(current)).toList(),
                    List.of(),
                    PAGE_BODY_HEIGHT);
        }

        private List<List<PdfLine>> finish() {
            if (current.stream().noneMatch(this::drawsText)) {
                return done;
            }
            return Stream.concat(done.stream(), Stream.of(current)).toList();
        }

        private boolean drawsText(final PdfLine line) {
            if (line.table()) {
                return line.cells().stream().flatMap(List::stream).anyMatch((final String cell) -> !cell.isBlank());
            }
            return line.role() != Role.SPACER && line.role() != Role.RULE && !line.text().isEmpty();
        }

        private static PageAcc sequential(final PageAcc left, final PageAcc ignored) {
            return left;
        }
    }

    private record BlockAcc(List<List<PdfLine>> done, List<PdfLine> current) {

        private static BlockAcc empty() {
            return new BlockAcc(List.of(), List.of());
        }

        private BlockAcc add(final PdfLine line) {
            if (line.role() == Role.PAGE_BREAK || line.role() == Role.SLOT || line.role() == Role.CATEGORY) {
                return close().open(line);
            }
            return new BlockAcc(done, Stream.concat(current.stream(), Stream.of(line)).toList());
        }

        private BlockAcc open(final PdfLine line) {
            return new BlockAcc(done, List.of(line));
        }

        private BlockAcc close() {
            if (current.isEmpty()) {
                return this;
            }
            return new BlockAcc(Stream.concat(done.stream(), Stream.of(current)).toList(), List.of());
        }

        private List<List<PdfLine>> finish() {
            return close().done();
        }

        private static BlockAcc sequential(final BlockAcc left, final BlockAcc ignored) {
            return left;
        }
    }
}
