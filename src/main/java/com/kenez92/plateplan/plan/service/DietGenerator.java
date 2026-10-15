package com.kenez92.plateplan.plan.service;

import java.util.List;

import com.kenez92.plateplan.plan.model.PlanPerson;
import com.kenez92.plateplan.plan.model.PlanResult;
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
 * The only owner of {@link ChatClient}. It sends the dietitian role, each selected person's name,
 * goal, and calories, and one merged pair of product lists, and returns a {@link WeekPlan}, or
 * {@link PlanResult#unavailable()} when the key is missing, the call times out, parsing fails, the
 * week is rejected, or HTTP fails. It does not retry. It does not log the key, the prompt, product
 * lists, calories, the goal, names, or meal text.
 */
@Service
public class DietGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DietGenerator.class);

    private static final String API_KEY_PROPERTY = "${spring.ai.ollama.api-key:}";
    private static final String FAILED_LOG = "Diet generation failed: {} (cause: {})";
    private static final String KEY_MISSING_LOG = "Diet generation failed: missing API key";
    private static final String EMPTY_PLAN_LOG = "Diet generation failed: empty model entity";
    private static final String REJECTED_WEEK_LOG = "Diet generation failed: week plan rejected";
    private static final String EMPTY_PRODUCTS = "none";
    private static final String PRODUCT_SEPARATOR = ", ";
    private static final String PERSON_JOINER = "\n";
    private static final String PERSON_LINE = "Name: %s. Goal: %s. Daily calorie target: %d kcal.";
    private static final String GOAL_LOSE_WEIGHT =
            "weight loss (calorie deficit): choose filling, protein-forward meals that support fat loss";
    private static final String GOAL_MAINTAIN = "weight maintenance: keep energy balance at the calorie target";
    private static final String GOAL_GAIN =
            "weight gain: choose calorie-dense, protein-forward meals that support gaining mass";
    private static final String PROMPT = """
            You are a dietitian. Create a diet for seven consecutive days. Reply entirely in Polish.
            Do not assign calendar dates.
            Return seven days. Each day has four meals: breakfast, secondBreakfast, lunch, and dinner, \
            plus one shoppingList for the whole week.
            Each meal has one shared non-empty dish name and one shared preparation, plus one portion \
            for each person named below and for nobody else.
            Each portion has personName exactly as given below, ingredients, kcal as an integer, and \
            proteinG, carbsG, and fatG as whole grams for that person's portion.
            For breakfast, lunch, and dinner, preparation is two or three short sentences with the \
            details a cook needs: temperature, time, and the pan, pot, or oven dish when they matter. \
            Name the oven or hob temperature when the meal is baked or fried. Do not stop at one vague \
            step, and do not write a long essay.
            Each ingredient must include name and amount (examples: 2 szt., 150 g, 1 opakowanie).
            shoppingList is the shop list summed across the seven days: the same name appears once, \
            with the amount summed by you, not repeated per meal, per day, or per person.
            Cook for these people, in this order, and for nobody else. For each one the line is the \
            name, the goal, and the daily calorie target:
            %s
            Each person's four portions on a day stay within ten percent of that person's own target. \
            The meals must match that person's goal, not only the calorie number.
            Breakfast, lunch, and dinner names must be unique across the seven days.
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
            """;

    private final ChatClient chatClient;
    private final WeekPlanValidator weekPlanValidator;
    private final String apiKey;

    public DietGenerator(final ChatClient.Builder chatClientBuilder,
                         final WeekPlanValidator weekPlanValidator,
                         @Value(API_KEY_PROPERTY) final String apiKey) {
        this.chatClient = chatClientBuilder.build();
        this.weekPlanValidator = weekPlanValidator;
        this.apiKey = apiKey;
    }

    public PlanResult generate(final List<PlanPerson> people,
                               final List<String> preferred,
                               final List<String> excluded) {
        if (apiKey.isBlank()) {
            LOGGER.warn(KEY_MISSING_LOG);
            return PlanResult.unavailable();
        }
        try {
            final WeekPlan weekPlan = chatClient.prompt()
                    .user(PROMPT.formatted(peopleText(people), names(preferred), names(excluded)))
                    .call()
                    .entity(WeekPlan.class);
            if (weekPlan == null) {
                LOGGER.warn(EMPTY_PLAN_LOG);
                return PlanResult.unavailable();
            }
            if (!weekPlanValidator.isValid(weekPlan, people)) {
                LOGGER.warn(REJECTED_WEEK_LOG);
                return PlanResult.unavailable();
            }
            return PlanResult.success(weekPlan, people);
        } catch (final Exception exception) {
            logFailure(exception);
            return PlanResult.unavailable();
        }
    }

    private String peopleText(final List<PlanPerson> people) {
        return String.join(PERSON_JOINER, people.stream().map(this::personLine).toList());
    }

    private String personLine(final PlanPerson person) {
        return PERSON_LINE.formatted(person.name(), goalInstruction(person.goal()), person.kcal());
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
}
