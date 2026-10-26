package com.kenez92.plateplan.plan.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.kenez92.plateplan.plan.model.DayMeals;
import com.kenez92.plateplan.plan.model.Ingredient;
import com.kenez92.plateplan.plan.model.Meal;
import com.kenez92.plateplan.plan.model.PlanPerson;
import com.kenez92.plateplan.plan.model.Portion;
import com.kenez92.plateplan.plan.model.ShoppingItem;
import com.kenez92.plateplan.plan.model.WeekPlan;
import org.springframework.stereotype.Component;

/**
 * Puts a parsed week into the shape the validator accepts when the model returned seven days and
 * one portion per selected person on every meal. Portion names are the account names. Each person's
 * four portions are scaled so the day equals that person's target. A week with the wrong number of
 * days or portions is left alone and comes back as null. Dish names stay as the model wrote them.
 * The shopping list is rebuilt from every ingredient on every portion, with the same product summed
 * once. A category the model already gave for that product is kept.
 */
@Component
public class WeekPlanRepair {

    private static final int DAYS_IN_WEEK = 7;
    private static final int BREAKFAST = 0;
    private static final int SECOND_BREAKFAST = 1;
    private static final int LUNCH = 2;
    private static final int DINNER = 3;
    private static final int WHOLE_SCALE = 0;
    private static final int NO_CALORIES = 0;
    private static final int SINGLE_AMOUNT = 1;
    private static final int FIRST_CHARACTER = 0;
    private static final int DIGIT_ZERO = '0';
    private static final int DIGIT_NINE = '9';
    private static final String EMPTY = "";
    private static final String AMOUNT_JOINER = ", ";
    private static final String NUMBER_AND_UNIT = "%s %s";
    private static final String REPEATED_AMOUNT = "%d× %s";
    private static final char DECIMAL_COMMA = ',';
    private static final char DECIMAL_POINT = '.';

    /**
     * The repaired week, or null when the days or the portions cannot be lined up with the people.
     */
    public WeekPlan repair(final WeekPlan weekPlan, final List<PlanPerson> people) {
        if (!canRepair(weekPlan, people)) {
            return null;
        }
        final List<DayMeals> days = weekPlan.days().stream()
                .map((final DayMeals day) -> repairDay(day, people))
                .toList();
        return new WeekPlan(days, shoppingList(days, weekPlan.shoppingList()));
    }

    private boolean canRepair(final WeekPlan weekPlan, final List<PlanPerson> people) {
        return weekPlan != null
                && people != null
                && !people.isEmpty()
                && weekPlan.days() != null
                && weekPlan.days().size() == DAYS_IN_WEEK
                && weekPlan.days().stream().allMatch((final DayMeals day) -> day != null && portionsFit(day, people.size()));
    }

    private boolean portionsFit(final DayMeals day, final int count) {
        return day.breakfast().portions().size() == count
                && day.secondBreakfast().portions().size() == count
                && day.lunch().portions().size() == count
                && day.dinner().portions().size() == count;
    }

    private DayMeals repairDay(final DayMeals day, final List<PlanPerson> people) {
        final List<Portion> breakfast = aligned(day.breakfast().portions(), people);
        final List<Portion> secondBreakfast = aligned(day.secondBreakfast().portions(), people);
        final List<Portion> lunch = aligned(day.lunch().portions(), people);
        final List<Portion> dinner = aligned(day.dinner().portions(), people);
        final List<List<Portion>> scaled = IntStream.range(0, people.size())
                .mapToObj((final int index) -> scalePerson(List.of(
                        breakfast.get(index),
                        secondBreakfast.get(index),
                        lunch.get(index),
                        dinner.get(index)), people.get(index).kcal()))
                .toList();
        return new DayMeals(
                meal(day.breakfast(), column(scaled, BREAKFAST)),
                meal(day.secondBreakfast(), column(scaled, SECOND_BREAKFAST)),
                meal(day.lunch(), column(scaled, LUNCH)),
                meal(day.dinner(), column(scaled, DINNER)));
    }

    private Meal meal(final Meal meal, final List<Portion> portions) {
        return new Meal(meal.name(), meal.preparation(), portions);
    }

    private List<Portion> column(final List<List<Portion>> byPerson, final int mealIndex) {
        return byPerson.stream().map((final List<Portion> portions) -> portions.get(mealIndex)).toList();
    }

    private List<Portion> aligned(final List<Portion> portions, final List<PlanPerson> people) {
        if (people.stream().allMatch((final PlanPerson person) -> portions.stream()
                .anyMatch((final Portion portion) -> samePerson(portion.personName(), person.name())))) {
            return people.stream()
                    .map((final PlanPerson person) -> portions.stream()
                            .filter((final Portion portion) -> samePerson(portion.personName(), person.name()))
                            .findFirst()
                            .map((final Portion portion) -> named(portion, person.name()))
                            .orElseThrow())
                    .toList();
        }
        return IntStream.range(0, people.size())
                .mapToObj((final int index) -> named(portions.get(index), people.get(index).name()))
                .toList();
    }

    private List<Portion> scalePerson(final List<Portion> portions, final int target) {
        final List<Integer> kcal = kcalTargets(portions, target);
        return IntStream.range(0, portions.size())
                .mapToObj((final int index) -> scaled(portions.get(index), kcal.get(index)))
                .toList();
    }

    private List<Integer> kcalTargets(final List<Portion> portions, final int target) {
        final int total = portions.stream().mapToInt(Portion::kcal).sum();
        if (total <= NO_CALORIES) {
            return evenSplit(target, portions.size());
        }
        return withDrift(portions.stream()
                .map((final Portion portion) -> divide(portion.kcal(), total, target))
                .toList(), target);
    }

    private List<Integer> evenSplit(final int target, final int count) {
        final int base = target / count;
        final int remainder = target % count;
        return IntStream.range(0, count)
                .mapToObj((final int index) -> base + (index < remainder ? 1 : 0))
                .toList();
    }

    private List<Integer> withDrift(final List<Integer> scaled, final int target) {
        final int drift = target - scaled.stream().mapToInt(Integer::intValue).sum();
        if (drift == NO_CALORIES) {
            return scaled;
        }
        final int index = IntStream.range(0, scaled.size())
                .reduce((final int left, final int right) -> scaled.get(left) >= scaled.get(right) ? left : right)
                .orElse(BREAKFAST);
        return IntStream.range(0, scaled.size())
                .mapToObj((final int position) -> position == index ? scaled.get(position) + drift : scaled.get(position))
                .toList();
    }

    private int divide(final int kcal, final int total, final int target) {
        return new BigDecimal(kcal)
                .multiply(new BigDecimal(target))
                .divide(new BigDecimal(total), WHOLE_SCALE, RoundingMode.HALF_UP)
                .intValueExact();
    }

    private Portion scaled(final Portion portion, final int kcal) {
        return new Portion(
                portion.personName(),
                portion.ingredients(),
                kcal,
                macro(portion.proteinG(), portion.kcal(), kcal),
                macro(portion.carbsG(), portion.kcal(), kcal),
                macro(portion.fatG(), portion.kcal(), kcal));
    }

    private int macro(final int grams, final int originalKcal, final int kcal) {
        if (originalKcal == NO_CALORIES) {
            return grams;
        }
        return new BigDecimal(grams)
                .multiply(new BigDecimal(kcal))
                .divide(new BigDecimal(originalKcal), WHOLE_SCALE, RoundingMode.HALF_UP)
                .intValueExact();
    }

    private Portion named(final Portion portion, final String personName) {
        return new Portion(personName, portion.ingredients(), portion.kcal(), portion.proteinG(), portion.carbsG(),
                portion.fatG());
    }

    private boolean samePerson(final String left, final String right) {
        return sameName(left, right);
    }

    private boolean sameName(final String left, final String right) {
        return nameKey(left).equals(nameKey(right));
    }

    private String nameKey(final String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }

    private List<ShoppingItem> shoppingList(final List<DayMeals> days, final List<ShoppingItem> given) {
        final List<Ingredient> ingredients = days.stream()
                .flatMap(this::meals)
                .flatMap((final Meal meal) -> meal.portions().stream())
                .flatMap((final Portion portion) -> portion.ingredients().stream())
                .filter((final Ingredient ingredient) -> !ingredient.name().isBlank())
                .toList();
        return ingredients.stream()
                .map(Ingredient::name)
                .reduce(List.of(), this::rememberName, (final List<String> left, final List<String> ignored) -> left)
                .stream()
                .map((final String name) -> new ShoppingItem(name, summed(ingredients, name), category(given, name)))
                .toList();
    }

    private Stream<Meal> meals(final DayMeals day) {
        return Stream.of(day.breakfast(), day.secondBreakfast(), day.lunch(), day.dinner());
    }

    private List<String> rememberName(final List<String> names, final String name) {
        if (names.stream().anyMatch((final String existing) -> sameName(existing, name))) {
            return names;
        }
        return Stream.concat(names.stream(), Stream.of(name)).toList();
    }

    private String summed(final List<Ingredient> ingredients, final String name) {
        final List<String> amounts = ingredients.stream()
                .filter((final Ingredient ingredient) -> sameName(ingredient.name(), name))
                .map(Ingredient::amount)
                .filter((final String amount) -> !amount.isBlank())
                .toList();
        if (amounts.isEmpty()) {
            return EMPTY;
        }
        return sumAmounts(amounts);
    }

    private String category(final List<ShoppingItem> given, final String name) {
        return given.stream()
                .filter((final ShoppingItem item) -> sameName(item.name(), name))
                .map(ShoppingItem::category)
                .findFirst()
                .orElse(EMPTY);
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

    private record Measured(BigDecimal number, String unit, String text) {
    }
}
