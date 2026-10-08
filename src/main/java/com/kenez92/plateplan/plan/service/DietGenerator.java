package com.kenez92.plateplan.plan.service;

import java.util.List;

import com.kenez92.plateplan.plan.model.DietPlan;
import com.kenez92.plateplan.plan.model.PlanResult;
import com.kenez92.plateplan.profile.model.Goal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Service;

/**
 * The only owner of {@link ChatClient}. It sends the dietitian role, the goal, calories, and
 * product lists and returns a {@link DietPlan}, or {@link PlanResult#unavailable()} when the key
 * is missing, the call times out, parsing fails, or HTTP fails. It does not log the key, the
 * prompt, product lists, calories, or the goal.
 */
@Service
public class DietGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DietGenerator.class);

    private static final String API_KEY_PROPERTY = "${spring.ai.ollama.api-key:}";
    private static final String FAILED_LOG = "Diet generation failed: {} (cause: {})";
    private static final String KEY_MISSING_LOG = "Diet generation failed: missing API key";
    private static final String EMPTY_PLAN_LOG = "Diet generation failed: empty model entity";
    private static final String EMPTY_PRODUCTS = "none";
    private static final String PRODUCT_SEPARATOR = ", ";
    private static final String GOAL_LOSE_WEIGHT =
            "weight loss (calorie deficit): choose filling, protein-forward meals that support fat loss";
    private static final String GOAL_MAINTAIN = "weight maintenance: keep energy balance at the calorie target";
    private static final String GOAL_GAIN =
            "weight gain: choose calorie-dense, protein-forward meals that support gaining mass";
    private static final String PROMPT = """
            You are a dietitian. Create a next-day diet plan. Reply entirely in Polish.
            Return four meals: breakfast, secondBreakfast, lunch, and dinner, plus a shoppingList.
            Each meal must include a dish name, ingredients, preparation as short cooking or serving \
            steps in Polish, kcal as an integer, and proteinG, carbsG, and fatG as whole grams \
            for that meal.
            Each ingredient must include name and amount (examples: 2 szt., 150 g, 1 opakowanie).
            shoppingList is the combined shop list for the whole day: each item has name and \
            amount to buy, summed across meals, not repeated per meal.
            The client's goal is %s. The meals must match that goal, not only the calorie number.
            Stay close to the daily calorie target of %d kcal.
            secondBreakfast must be a ready-made shop item, not a cooked meal. Examples: drinkable \
            skyr, a Go Active salad, yogurt, or a cheese snack. Its preparation says how to serve \
            it, not how to cook it.
            Preferred products are preferences only: use them when they fit, but other foods are \
            allowed and the names need not match exactly.
            Never use the excluded products.
            Product names below are in the user's language as typed; treat them as those foods and \
            do not translate them into different foods.
            Preferred products: %s.
            Excluded products: %s.
            """;

    private final ChatClient chatClient;
    private final String apiKey;

    public DietGenerator(final ChatClient.Builder chatClientBuilder,
                         @Value(API_KEY_PROPERTY) final String apiKey) {
        this.chatClient = chatClientBuilder.build();
        this.apiKey = apiKey;
    }

    public PlanResult generate(final int dailyCalories,
                               final Goal goal,
                               final List<String> preferred,
                               final List<String> excluded) {
        if (apiKey.isBlank()) {
            LOGGER.warn(KEY_MISSING_LOG);
            return PlanResult.unavailable();
        }
        try {
            final DietPlan dietPlan = chatClient.prompt()
                    .user(PROMPT.formatted(goalInstruction(goal), dailyCalories, names(preferred), names(excluded)))
                    .call()
                    .entity(DietPlan.class);
            if (dietPlan == null) {
                LOGGER.warn(EMPTY_PLAN_LOG);
                return PlanResult.unavailable();
            }
            return PlanResult.success(dietPlan, dailyCalories);
        } catch (final Exception exception) {
            logFailure(exception);
            return PlanResult.unavailable();
        }
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
