package com.kenez92.plateplan.plan.service;

import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;
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
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Service;

/**
 * The only owner of {@link ChatClient}. It asks for ten candidate days and keeps up to seven that
 * fit after one scaling. It asks up to five times, and stops when seven days are kept. A shorter
 * plan is returned when at least one day fits. Each person line carries that person's fat cap.
 * It returns a {@link WeekPlan}, or {@link PlanResult#unavailable()} when a call fails or five
 * answers still yield no day. A reply that stops mid-day still keeps the days that were finished.
 * Liked and disliked dish names go into the prompt. A disliked dish is not a reason to reject the
 * week. It does not log the prompt, the cap, kilocalories, the goal, product lists, dish names, or
 * grams.
 */
@Service
public class DietGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DietGenerator.class);

    private static final String FAILED_LOG = "Diet generation failed: {} (cause: {})";
    private static final String EMPTY_PLAN_LOG = "Diet generation failed: empty model entity";
    private static final String REJECTED_WEEK_LOG =
            "Diet generation failed: kept {} of {} parsed days, {} unaligned";
    private static final String SHORT_WEEK_LOG = "Diet generation kept {} days";
    private static final int DAYS_IN_PLAN = 7;
    private static final int ATTEMPTS = 5;
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
            Dish names and preparation are Polish. JSON property names stay English: days, breakfast, \
            secondBreakfast, lunch, dinner, shoppingList, name, preparation, portions, personName, \
            ingredients, amount, kcal, proteinG, carbsG, fatG, category. Do not translate those names.
            Return one complete JSON object, with no markdown fence, and do not stop in the middle of \
            a day or a list.
            Do not assign calendar dates.
            Return ten days. Each day has four meals: breakfast, secondBreakfast, lunch, and dinner, \
            plus one shoppingList for the whole week.
            Each meal has one shared non-empty dish name and one shared preparation, plus one portion \
            for each person named below and for nobody else.
            Each portion has personName exactly as given below, ingredients, kcal as an integer, and \
            proteinG, carbsG, and fatG as whole grams for that person's portion. Write kcal, proteinG, \
            carbsG, and fatG as JSON integers, never decimals and never quoted strings. Write every \
            amount as a string such as "150 g" or "2 szt.", never as a bare number.
            For breakfast, lunch, and dinner, preparation is a short sequence of steps in cooking order. \
            Each step is one sentence, with the temperature, time, and the pan, pot, or oven dish when \
            they matter. When the meal uses an oven, also name the mode: góra-dół for top and bottom heat, \
            termoobieg for fan-assisted heat, or grill. Keep each cooked preparation to at most three \
            short sentences so all ten days fit. Do not stop at one vague step, and do not write \
            a long essay. The dish name and the preparation may name only foods listed as ingredients \
            of that meal. Do not mention a food you left out. Every cooked ingredient is cooked in \
            the preparation.
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
            Write those portions already close to that target. Fat grams are scaled with the calories, \
            so a high fat share on a small portion goes over the cap once the day is at the target.
            Each person's four portions on a day stay at or under that person's fat cap. Fat from sauce, \
            oil, and butter counts in the sum. Before you finish a day, add that person's four fatG \
            values. If the sum is over the fat cap on their line, reduce oil, butter, cheese, nuts, and \
            fatty meat until the sum is at or under the cap. Do not return a day that is over. Use a \
            lean cut and a teaspoon of fat to season, not a pool of oil.
            Breakfast, lunch, and dinner names must be unique across the ten days. A repeated breakfast, \
            lunch, or dinner name is dropped, so write a new name instead of repeating one.
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
            Dishes this account liked: %s.
            Dishes this account disliked: %s.
            Cook in the spirit of the liked dishes when they fit the day. Do not serve a disliked dish.
            """;

    private final ChatClient chatClient;
    private final WeekPlanValidator weekPlanValidator;
    private final WeekPlanRepair weekPlanRepair;
    private final WeekPlanOutput weekPlanOutput;

    public DietGenerator(final ChatClient.Builder chatClientBuilder,
                         final WeekPlanValidator weekPlanValidator,
                         final WeekPlanRepair weekPlanRepair,
                         final WeekPlanOutput weekPlanOutput) {
        this.chatClient = chatClientBuilder.build();
        this.weekPlanValidator = weekPlanValidator;
        this.weekPlanRepair = weekPlanRepair;
        this.weekPlanOutput = weekPlanOutput;
    }

    public PlanResult generate(final List<PlanPerson> people,
                               final List<String> preferred,
                               final List<String> excluded,
                               final List<String> avoided) {
        return generate(people, preferred, excluded, avoided, List.of(), List.of());
    }

    public PlanResult generate(final List<PlanPerson> people,
                               final List<String> preferred,
                               final List<String> excluded,
                               final List<String> avoided,
                               final List<String> liked,
                               final List<String> disliked) {
        final String prompt = PROMPT.formatted(
                peopleText(people), names(preferred), names(excluded), names(avoided),
                names(liked), names(disliked));
        final Gather gathered = IntStream.range(0, ATTEMPTS)
                .boxed()
                .reduce(Gather.empty(),
                        (final Gather gather, final Integer ignored) -> next(gather, prompt, people, avoided),
                        (final Gather left, final Gather ignored) -> left);
        if (gathered.days().isEmpty()) {
            if (gathered.answered()) {
                LOGGER.warn(REJECTED_WEEK_LOG, gathered.days().size(), gathered.parsedDays(), gathered.unaligned());
            }
            return PlanResult.unavailable();
        }
        if (gathered.days().size() < DAYS_IN_PLAN) {
            LOGGER.warn(SHORT_WEEK_LOG, gathered.days().size());
        }
        return accepted(gathered.days(), gathered.shopping(), people);
    }

    private Gather next(final Gather gather,
                        final String prompt,
                        final List<PlanPerson> people,
                        final List<String> avoided) {
        if (gather.days().size() >= DAYS_IN_PLAN) {
            return gather;
        }
        final Attempt attempt = attempt(prompt, people);
        return gather.plus(select(
                Stream.concat(gather.days().stream(), attempt.days().stream()).toList(), avoided), attempt);
    }

    private Attempt attempt(final String prompt, final List<PlanPerson> people) {
        try {
            final WeekPlan parsed = chatClient.prompt().user(prompt).call().entity(weekPlanOutput);
            if (parsed == null) {
                LOGGER.warn(EMPTY_PLAN_LOG);
                return Attempt.none();
            }
            return new Attempt(goodDays(parsed, people), shopping(parsed), true,
                    parsed.days() == null ? 0 : parsed.days().size(), unaligned(parsed, people));
        } catch (final Exception exception) {
            logFailure(exception);
            return Attempt.none();
        }
    }

    private int unaligned(final WeekPlan weekPlan, final List<PlanPerson> people) {
        if (weekPlan.days() == null) {
            return 0;
        }
        return (int) weekPlan.days().stream()
                .filter((final DayMeals day) -> weekPlanRepair.scaleDay(day, people) == null)
                .count();
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

    private record Gather(List<DayMeals> days, List<ShoppingItem> shopping, boolean answered,
                          int parsedDays, int unaligned) {

        private static Gather empty() {
            return new Gather(List.of(), List.of(), false, 0, 0);
        }

        private Gather plus(final List<DayMeals> kept, final Attempt attempt) {
            return new Gather(kept,
                    Stream.concat(shopping.stream(), attempt.shopping().stream()).toList(),
                    answered || attempt.answered(),
                    parsedDays + attempt.parsedDays(),
                    unaligned + attempt.unaligned());
        }
    }

    private record Attempt(List<DayMeals> days, List<ShoppingItem> shopping, boolean answered,
                           int parsedDays, int unaligned) {

        private static Attempt none() {
            return new Attempt(List.of(), List.of(), false, 0, 0);
        }
    }

    private record Selection(List<DayMeals> days, List<String> names) {

        private static Selection none() {
            return new Selection(List.of(), List.of());
        }
    }
}
