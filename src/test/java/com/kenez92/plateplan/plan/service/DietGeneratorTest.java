package com.kenez92.plateplan.plan.service;

import java.util.List;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.kenez92.plateplan.plan.model.DietPlan;
import com.kenez92.plateplan.plan.model.Ingredient;
import com.kenez92.plateplan.plan.model.Meal;
import com.kenez92.plateplan.plan.model.PlanResult;
import com.kenez92.plateplan.plan.model.ShoppingItem;
import com.kenez92.plateplan.profile.model.Goal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DietGeneratorTest {

    @Test
    void shouldReturnTheDietPlanWhenTheModelAnswers() {
        final DietPlan expectedPlan = samplePlan();
        final ChatClient chatClient = chatClientReturning(expectedPlan);
        final DietGenerator generator = generator(chatClient, "ollama-key");

        final PlanResult actual = generator.generate(2000, Goal.MAINTAIN, List.of("jajka"), List.of("orzechy"));

        final PlanResult expected = PlanResult.success(expectedPlan, 2000);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnUnavailableWhenTheApiKeyIsMissing() {
        final ChatClient chatClient = mock(ChatClient.class);
        final DietGenerator generator = generator(chatClient, "");

        final PlanResult actual = generator.generate(2000, Goal.MAINTAIN, List.of("jajka"), List.of("orzechy"));

        verify(chatClient, never()).prompt();
        final PlanResult expected = PlanResult.unavailable();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldLogWhenTheApiKeyIsMissing() {
        final ChatClient chatClient = mock(ChatClient.class);
        final DietGenerator generator = generator(chatClient, "");
        final Logger logger = (Logger) LoggerFactory.getLogger(DietGenerator.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            generator.generate(2000, Goal.MAINTAIN, List.of("jajka"), List.of("orzechy"));
        } finally {
            logger.detachAppender(appender);
        }

        final List<String> messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)).contains("missing API key");
        assertThat(messages.get(0)).doesNotContain("jajka", "orzechy", "2000");
    }

    @Test
    void shouldReturnUnavailableWhenTheModelReturnsNothing() {
        final ChatClient chatClient = chatClientReturning(null);
        final DietGenerator generator = generator(chatClient, "ollama-key");

        final PlanResult actual = generator.generate(2000, Goal.MAINTAIN, List.of("jajka"), List.of("orzechy"));

        final PlanResult expected = PlanResult.unavailable();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldLogWhenTheModelReturnsNothing() {
        final ChatClient chatClient = chatClientReturning(null);
        final DietGenerator generator = generator(chatClient, "ollama-key");
        final Logger logger = (Logger) LoggerFactory.getLogger(DietGenerator.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            generator.generate(2000, Goal.MAINTAIN, List.of("jajka"), List.of("orzechy"));
        } finally {
            logger.detachAppender(appender);
        }

        final List<String> messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)).contains("empty model entity");
        assertThat(messages.get(0)).doesNotContain("jajka", "orzechy", "ollama-key", "2000");
    }

    @Test
    void shouldReturnUnavailableWhenTheModelCallFails() {
        final ChatClient.ChatClientRequestSpec spec = mock(ChatClient.ChatClientRequestSpec.class);
        final ChatClient chatClient = mock(ChatClient.class);
        when(chatClient.prompt()).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenThrow(new RuntimeException("The model is unreachable"));
        final DietGenerator generator = generator(chatClient, "ollama-key");

        final PlanResult actual = generator.generate(2000, Goal.MAINTAIN, List.of("jajka"), List.of("orzechy"));

        final PlanResult expected = PlanResult.unavailable();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldLogOnlyTheClassNamesWhenTheModelCallFails() {
        final ChatClient.ChatClientRequestSpec spec = mock(ChatClient.ChatClientRequestSpec.class);
        final ChatClient chatClient = mock(ChatClient.class);
        when(chatClient.prompt()).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenThrow(new RuntimeException("ollama-key, 2000, jajka"));
        final DietGenerator generator = generator(chatClient, "ollama-key");
        final Logger logger = (Logger) LoggerFactory.getLogger(DietGenerator.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            generator.generate(2000, Goal.MAINTAIN, List.of("jajka"), List.of("orzechy"));
        } finally {
            logger.detachAppender(appender);
        }

        final List<String> messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)).contains(RuntimeException.class.getName());
        assertThat(messages.get(0)).doesNotContain("ollama-key", "2000", "jajka");
    }

    @Test
    void shouldIncludeRoleGoalCaloriesAndProductNamesAndOmitPersonalDataFromThePrompt() {
        final ChatClient.ChatClientRequestSpec spec = mock(ChatClient.ChatClientRequestSpec.class);
        final ChatClient.CallResponseSpec call = mock(ChatClient.CallResponseSpec.class);
        final ChatClient chatClient = mock(ChatClient.class);
        when(chatClient.prompt()).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenReturn(call);
        when(call.entity(eq(DietPlan.class))).thenReturn(samplePlan());
        final DietGenerator generator = generator(chatClient, "ollama-key");

        generator.generate(2000, Goal.LOSE_WEIGHT, List.of("jajka"), List.of("orzechy"));

        final ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(spec).user(prompt.capture());
        assertThat(prompt.getValue()).contains("dietitian", "weight loss", "calorie deficit", "2000", "jajka",
                "orzechy", "ready-made shop item", "preferences only", "user's language as typed", "proteinG",
                "carbsG", "fatG", "amount", "preparation");
        assertThat(prompt.getValue()).doesNotContain("alice", "34", "180", "82.5", "MALE", "LOSE_WEIGHT", "MODERATE");
    }

    private DietGenerator generator(final ChatClient chatClient, final String apiKey) {
        final ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(chatClient);
        return new DietGenerator(builder, apiKey);
    }

    private ChatClient chatClientReturning(final DietPlan dietPlan) {
        final ChatClient.ChatClientRequestSpec spec = mock(ChatClient.ChatClientRequestSpec.class);
        final ChatClient.CallResponseSpec call = mock(ChatClient.CallResponseSpec.class);
        final ChatClient chatClient = mock(ChatClient.class);
        when(chatClient.prompt()).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenReturn(call);
        when(call.entity(eq(DietPlan.class))).thenReturn(dietPlan);
        return chatClient;
    }

    private DietPlan samplePlan() {
        return new DietPlan(
                new Meal("Jajecznica", List.of(new Ingredient("jajka", "2 szt."), new Ingredient("chleb", "60 g")),
                        450, 28, 32, 22, "Rozbij jajka i usmaż na patelni."),
                new Meal("Jogurt", List.of(new Ingredient("jogurt", "200 g"), new Ingredient("banan", "1 szt.")),
                        250, 18, 30, 4, "Otwórz jogurt i dodaj banana."),
                new Meal("Schabowy", List.of(new Ingredient("schab", "150 g"), new Ingredient("ziemniaki", "200 g")),
                        800, 40, 55, 35, "Usmaż schab i ugotuj ziemniaki."),
                new Meal("Zupa", List.of(new Ingredient("warzywa", "300 g")), 500, 12, 40, 18,
                        "Gotuj warzywa w wodzie do miękkości."),
                List.of(
                        new ShoppingItem("jajka", "2 szt."),
                        new ShoppingItem("chleb", "60 g"),
                        new ShoppingItem("jogurt", "200 g"),
                        new ShoppingItem("banan", "1 szt."),
                        new ShoppingItem("schab", "150 g"),
                        new ShoppingItem("ziemniaki", "200 g"),
                        new ShoppingItem("warzywa", "300 g")));
    }
}
