package com.kenez92.plateplan.plan.service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import com.kenez92.plateplan.plan.model.DayMeals;
import com.kenez92.plateplan.plan.model.PlanPerson;
import com.kenez92.plateplan.plan.model.PlanResult;
import com.kenez92.plateplan.plan.model.ShoppingItem;
import com.kenez92.plateplan.plan.model.WeekPlan;
import com.kenez92.plateplan.plan.validator.WeekPlanValidator;
import com.kenez92.plateplan.profile.model.Goal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Service;

/**
 * The only owner of {@link ChatClient}. It asks for ten candidate days and keeps seven that fit
 * after one scaling, with one follow-up when the first answer is short. Each person line carries
 * that person's fat cap. It returns a {@link WeekPlan}, or {@link PlanResult#unavailable()} when
 * the key is missing, a call fails, or two answers still do not yield seven days. It does not log
 * the key, the prompt, the cap, kilocalories, the goal, product lists, dish names, or grams.
 */
@Service
public class DietGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DietGenerator.class);

    private static final String API_KEY_PROPERTY = "${spring.ai.ollama.api-key:}";
    private static final String FAILED_LOG = "Diet generation failed: {} (cause: {})";
    private static final String KEY_MISSING_LOG = "Diet generation failed: missing API key";
    private static final String EMPTY_PLAN_LOG = "Diet generation failed: empty model entity";
    private static final String REJECTED_WEEK_LOG = "Diet generation failed: week plan rejected";
    private static final int DAYS_IN_PLAN = 7;
    private static final String EMPTY_PRODUCTS = "none";
    private static final String PRODUCT_SEPARATOR = ", ";
    private static final String PERSON_JOINER = "\n";
    private static final String PERSON_LINE =
            "Name: %s. Goal: %s. Daily calorie target: %d kcal. Fat cap: %d g.";
    private static final String GOAL_LOSE_WEIGHT =
            "weight loss (calorie deficit): choose filling, protein-forward meals that support fat loss";
    private static final String GOAL_MAINTAIN = "weight maintenance: keep energy balance at the calorie target";
    private static final String GOAL_GAIN =
            "weight gain: choose calorie-dense, protein-forward meals that support gaining mass";
    private static final String PROMPT = """
            You are a dietitian. Create a diet for ten consecutive days. Reply entirely in Polish.
            Do not assign calendar dates.
            Return ten days. Each day has four meals: breakfast, secondBreakfast, lunch, and dinner, \
            plus one shoppingList for the whole week.
            Each meal has one shared non-empty dish name and one shared preparation, plus one portion \
            for each person named below and for nobody else.
            Each portion has personName exactly as given below, ingredients, kcal as an integer, and \
            proteinG, carbsG, and fatG as whole grams for that person's portion.
            For breakfast, lunch, and dinner, preparation is a short sequence of steps in cooking order. \
            Each step is one sentence, with the temperature, time, and the pan, pot, or oven dish when \
            they matter. When the meal uses an oven, also name the mode: góra-dół for top and bottom heat, \
            termoobieg for fan-assisted heat, or grill. Do not stop at one vague step, and do not write \
            a long essay.
            Each ingredient must include name and amount (examples: 2 szt., 150 g, 1 opakowanie). \
            Every food used in the meal is an ingredient. The amount is only that person's share. \
            Name the specific food a shopper buys. For meat, poultry, and fish name the cut: filet z indyka \
            or udko, not mięso z indyka. Do the same for a named cheese, a named bread, or a named fish.
            Breakfast, lunch, and dinner are one shared pot, pan, or baking dish. Every person gets a \
            share of that same dish. Use the same ingredient names for every portion; only the amounts \
            change with that person's calories.
            Season those meals so they taste complete and worth eating again. Name salt, pepper, herbs \
            or spices, and a sauce, fat, or acid such as lemon, vinegar, or tomato in the ingredients, \
            and use them in the preparation. Do not stop at a plain protein with one plain vegetable.
            Within each person's calorie target, the person feels full. Do not spend the calories on a \
            small dense portion.
            Breakfast is a morning meal: eggs, oatmeal, porridge, pancakes, cottage cheese, or bread with \
            a topping. It is not a roast, a stew, or meat with potatoes.
            Lunch is the main cooked meal, a proper obiad. Give it a sauce and a bulky side such as \
            potatoes, groats, rice, or pasta, plus a vegetable or a soup. Include at least two baked \
            one-dish meals such as a zapiekanka, and put them at lunch.
            Dinner is an evening kolacja, not a second lunch. No roast, no stew, and no meat with potatoes \
            or groats. Use eggs, soup, a salad, sandwiches, cottage cheese, an omelette, or a simple pasta \
            or fish, and make the plate large enough with bread, vegetables, soup, or salad.
            Vary the week. Use different methods, cuisines, and flavour profiles, and do not repeat the \
            same pattern of meat or fish plus one vegetable.
            shoppingList is the shop list summed across the ten days: the same name appears once, \
            with the amount summed by you, not repeated per meal, per day, or per person. \
            Each shopping item has a category, exactly one of: Warzywa i owoce, Pieczywo, Nabiał, Mięso, \
            Wędliny, Ryby, Produkty zbożowe, Tłuszcze, Przyprawy, Inne. \
            Vegetables and fruit are Warzywa i owoce. Bread is Pieczywo. Milk, cheese, yogurt, and eggs \
            are Nabiał. Raw meat and poultry are Mięso. Ham, sausage, and other cold cuts are Wędliny. \
            Fish and seafood are Ryby. Rice, groats, pasta, flour, and oats are Produkty zbożowe. \
            Oil and butter are Tłuszcze. Salt, pepper, dried herbs, and ground spices are Przyprawy. \
            Anything else is Inne. \
            Classify the food itself, not a word inside its name: sałata masłowa is a vegetable in \
            Warzywa i owoce, not a fat. Always include category as a string. When you are unsure, use Inne.
            Cook for these people, in this order, and for nobody else. For each one the line is the \
            name, the goal, the daily calorie target, and the fat cap in grams:
            %s
            Each person's four portions on a day stay within ten percent of that person's own target. \
            The meals must match that person's goal, not only the calorie number.
            Each person's four portions on a day stay at or under that person's fat cap. Fat from sauce, \
            oil, and butter counts in the sum.
            Breakfast, lunch, and dinner names must be unique across the ten days.
            secondBreakfast must be a ready-made shop item, not a cooked meal. Do not repeat the same \
            second breakfast without a break. Examples: drinkable \
            skyr, a Go Active salad, yogurt, or a cheese snack. Its preparation is one or two short \
            sentences on how to serve it, not how to cook it, with no oven and no temperature.
            Preferred products are preferences only: use them when they fit, but other foods are \
            allowed and the names need not match exactly.
            Never use the excluded products. An excluded product is excluded for every person at the table.
            Product names below are in the user's language as typed; treat them as those foods and \
            do not translate them into different foods.
            Preferred products: %s.
            Excluded products: %s.
            Do not use these names as lunch: %s.
            """;

    private final ChatClient chatClient;
    private final WeekPlanValidator weekPlanValidator;
    private final WeekPlanRepair weekPlanRepair;
    private final String apiKey;

    public DietGenerator(final ChatClient.Builder chatClientBuilder,
                         final WeekPlanValidator weekPlanValidator,
                         final WeekPlanRepair weekPlanRepair,
                         @Value(API_KEY_PROPERTY) final String apiKey) {
        this.chatClient = chatClientBuilder.build();
        this.weekPlanValidator = weekPlanValidator;
        this.weekPlanRepair = weekPlanRepair;
        this.apiKey = apiKey;
    }

    public PlanResult generate(final List<PlanPerson> people,
                               final List<String> preferred,
                               final List<String> excluded,
                               final List<String> avoided) {
        if (apiKey.isBlank()) {
            LOGGER.warn(KEY_MISSING_LOG);
            return PlanResult.unavailable();
        }
        final String prompt = PROMPT.formatted(
                peopleText(people), names(preferred), names(excluded), names(avoided));
        final Attempt first = attempt(prompt, people);
        final List<DayMeals> selected = select(first.days(), avoided);
        if (selected.size() == DAYS_IN_PLAN) {
            return accepted(selected, first.shopping(), people);
        }
        final Attempt second = attempt(prompt, people);
        final List<DayMeals> combined = select(joined(first, second), avoided);
        if (combined.size() == DAYS_IN_PLAN) {
            return accepted(combined, categories(first, second), people);
        }
        if (first.answered() || second.answered()) {
            LOGGER.warn(REJECTED_WEEK_LOG);
        }
        return PlanResult.unavailable();
    }

    private Attempt attempt(final String prompt, final List<PlanPerson> people) {
        try {
            final WeekPlan parsed = chatClient.prompt().user(prompt).call().entity(WeekPlan.class);
            if (parsed == null) {
                LOGGER.warn(EMPTY_PLAN_LOG);
                return Attempt.none();
            }
            return new Attempt(goodDays(parsed, people), shopping(parsed), true);
        } catch (final Exception exception) {
            logFailure(exception);
            return Attempt.none();
        }
    }

    private List<DayMeals> goodDays(final WeekPlan weekPlan, final List<PlanPerson> people) {
        if (weekPlan.days() == null) {
            return List.of();
        }
        return weekPlan.days().stream()
                .map((final DayMeals day) -> weekPlanRepair.scaleDay(day, people))
                .filter((final DayMeals day) -> day != null)
                .filter((final DayMeals day) -> weekPlanValidator.dayFits(day, people))
                .toList();
    }

    private List<DayMeals> select(final List<DayMeals> days, final List<String> avoided) {
        return days.stream()
                .reduce(Selection.none(),
                        (final Selection selection, final DayMeals day) -> take(selection, day, avoided),
                        (final Selection left, final Selection ignored) -> left)
                .days();
    }

    private Selection take(final Selection selection, final DayMeals day, final List<String> avoided) {
        if (selection.days().size() >= DAYS_IN_PLAN || !lunchAllowed(day, avoided) || !namesFit(selection, day)) {
            return selection;
        }
        return new Selection(
                Stream.concat(selection.days().stream(), Stream.of(day)).toList(),
                Stream.concat(selection.names().stream(), cookedNames(day).stream()).toList());
    }

    private boolean lunchAllowed(final DayMeals day, final List<String> avoided) {
        return weekPlanValidator.avoidsListedLunches(new WeekPlan(List.of(day), List.of()), avoided);
    }

    private boolean namesFit(final Selection selection, final DayMeals day) {
        final List<String> keys = cookedNames(day);
        return keys.stream().distinct().count() == keys.size()
                && keys.stream().noneMatch(selection.names()::contains);
    }

    private List<String> cookedNames(final DayMeals day) {
        return Stream.of(day.breakfast().name(), day.lunch().name(), day.dinner().name())
                .map(this::nameKey)
                .toList();
    }

    private String nameKey(final String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }

    private List<ShoppingItem> shopping(final WeekPlan weekPlan) {
        return weekPlan.shoppingList() == null ? List.of() : weekPlan.shoppingList();
    }

    private List<DayMeals> joined(final Attempt first, final Attempt second) {
        return Stream.concat(first.days().stream(), second.days().stream()).toList();
    }

    private List<ShoppingItem> categories(final Attempt first, final Attempt second) {
        return Stream.concat(first.shopping().stream(), second.shopping().stream()).toList();
    }

    private PlanResult accepted(final List<DayMeals> days,
                                final List<ShoppingItem> shopping,
                                final List<PlanPerson> people) {
        return PlanResult.success(new WeekPlan(days, weekPlanRepair.shoppingList(days, shopping)), people);
    }

    private String peopleText(final List<PlanPerson> people) {
        return String.join(PERSON_JOINER, people.stream().map(this::personLine).toList());
    }

    private String personLine(final PlanPerson person) {
        return PERSON_LINE.formatted(person.name(), goalInstruction(person.goal()), person.kcal(),
                weekPlanValidator.fatGramsLimit(person.kcal()));
    }

    private String goalInstruction(final Goal goal) {
        return switch (goal) {
            case LOSE_WEIGHT -> GOAL_LOSE_WEIGHT;
            case MAINTAIN -> GOAL_MAINTAIN;
            case GAIN -> GOAL_GAIN;
        };
    }

    private String names(final List<String> products) {
        if (products == null || products.isEmpty()) {
            return EMPTY_PRODUCTS;
        }
        return String.join(PRODUCT_SEPARATOR, products);
    }

    private void logFailure(final Exception exception) {
        LOGGER.warn(FAILED_LOG, exception.getClass().getName(), causeName(exception));
    }

    private String causeName(final Exception exception) {
        return NestedExceptionUtils.getMostSpecificCause(exception).getClass().getName();
    }

    private record Attempt(List<DayMeals> days, List<ShoppingItem> shopping, boolean answered) {

        private static Attempt none() {
            return new Attempt(List.of(), List.of(), false);
        }
    }

    private record Selection(List<DayMeals> days, List<String> names) {

        private static Selection none() {
            return new Selection(List.of(), List.of());
        }
    }
}
