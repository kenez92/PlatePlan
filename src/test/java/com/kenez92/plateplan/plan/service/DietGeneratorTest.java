package com.kenez92.plateplan.plan.service;

import java.util.List;
import java.util.stream.IntStream;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.kenez92.plateplan.plan.model.DayMeals;
import com.kenez92.plateplan.plan.model.Meal;
import com.kenez92.plateplan.plan.model.PlanPerson;
import com.kenez92.plateplan.plan.model.PlanResult;
import com.kenez92.plateplan.plan.model.Portion;
import com.kenez92.plateplan.plan.model.ShoppingItem;
import com.kenez92.plateplan.plan.model.WeekPlan;
import com.kenez92.plateplan.plan.validator.WeekPlanValidator;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DietGeneratorTest {

    @Test
    void shouldReturnTheWeekWhenTheModelAnswers() {
        final List<PlanPerson> people = List.of(new PlanPerson("Ja", Goal.MAINTAIN, 2000));
        final WeekPlan expectedPlan = acceptedWeek(people);
        final ChatClient chatClient = chatClientReturning(expectedPlan);
        final DietGenerator generator = generator(chatClient, "ollama-key");

        final PlanResult actual = generator.generate(people, List.of("jajka"), List.of("orzechy"));

        final PlanResult expected = PlanResult.success(expectedPlan, people);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnTheWeekWhenTheSameSecondBreakfastRepeatsEveryDay() {
        final List<PlanPerson> people = List.of(new PlanPerson("Ja", Goal.MAINTAIN, 2000));
        final WeekPlan expectedPlan = acceptedWeek(people);
        final ChatClient.CallResponseSpec call = mock(ChatClient.CallResponseSpec.class);
        final ChatClient chatClient = chatClientReturning(expectedPlan, call);
        final DietGenerator generator = generator(chatClient, "ollama-key");

        final PlanResult actual = generator.generate(people, List.of("jajka"), List.of("orzechy"));

        verify(call, times(1)).entity(eq(WeekPlan.class));
        final PlanResult expected = PlanResult.success(expectedPlan, people);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnUnavailableWhenTheApiKeyIsMissing() {
        final ChatClient chatClient = mock(ChatClient.class);
        final DietGenerator generator = generator(chatClient, "");

        final PlanResult actual = generator.generate(
                List.of(new PlanPerson("Ja", Goal.MAINTAIN, 2000)), List.of("jajka"), List.of("orzechy"));

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
            generator.generate(
                    List.of(new PlanPerson("Ja", Goal.MAINTAIN, 2000)), List.of("jajka"), List.of("orzechy"));
        } finally {
            logger.detachAppender(appender);
        }

        final List<String> messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)).contains("missing API key");
        assertThat(messages.get(0)).doesNotContain("jajka", "orzechy", "2000", "Ja", "weight maintenance");
    }

    @Test
    void shouldReturnUnavailableWhenTheModelReturnsNothing() {
        final ChatClient.CallResponseSpec call = mock(ChatClient.CallResponseSpec.class);
        final ChatClient chatClient = chatClientReturning(null, call);
        final DietGenerator generator = generator(chatClient, "ollama-key");

        final PlanResult actual = generator.generate(
                List.of(new PlanPerson("Ja", Goal.MAINTAIN, 2000)), List.of("jajka"), List.of("orzechy"));

        verify(call, times(1)).entity(eq(WeekPlan.class));
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
            generator.generate(
                    List.of(new PlanPerson("Ja", Goal.MAINTAIN, 2000)), List.of("jajka"), List.of("orzechy"));
        } finally {
            logger.detachAppender(appender);
        }

        final List<String> messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)).contains("empty model entity");
        assertThat(messages.get(0)).doesNotContain("jajka", "orzechy", "ollama-key", "2000", "Ja");
    }

    @Test
    void shouldReturnUnavailableWithoutASecondModelCallWhenTheValidatorRejectsTheWeek() {
        final ChatClient.CallResponseSpec call = mock(ChatClient.CallResponseSpec.class);
        final ChatClient chatClient = chatClientReturning(rejectedWeek(), call);
        final DietGenerator generator = generator(chatClient, "ollama-key");
        final Logger logger = (Logger) LoggerFactory.getLogger(DietGenerator.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        final PlanResult actual;
        try {
            actual = generator.generate(
                    List.of(new PlanPerson("Ja", Goal.MAINTAIN, 2000)), List.of("jajka"), List.of("orzechy"));
        } finally {
            logger.detachAppender(appender);
        }

        verify(call, times(1)).entity(eq(WeekPlan.class));
        verify(chatClient, times(1)).prompt();
        final PlanResult expected = PlanResult.unavailable();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
        assertThat(appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList())
                .allSatisfy(message -> assertThat(message)
                        .doesNotContain("Jajecznica", "Jogurt", "jajka", "orzechy", "2000", "ollama-key", "Ja",
                                "weight maintenance"));
    }

    @Test
    void shouldReturnUnavailableWhenTheModelCallFails() {
        final ChatClient.ChatClientRequestSpec spec = mock(ChatClient.ChatClientRequestSpec.class);
        final ChatClient chatClient = mock(ChatClient.class);
        when(chatClient.prompt()).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenThrow(new RuntimeException("The model is unreachable"));
        final DietGenerator generator = generator(chatClient, "ollama-key");

        final PlanResult actual = generator.generate(
                List.of(new PlanPerson("Ja", Goal.MAINTAIN, 2000)), List.of("jajka"), List.of("orzechy"));

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
            generator.generate(
                    List.of(new PlanPerson("Ja", Goal.MAINTAIN, 2000)), List.of("jajka"), List.of("orzechy"));
        } finally {
            logger.detachAppender(appender);
        }

        final List<String> messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)).contains(RuntimeException.class.getName());
        assertThat(messages.get(0)).doesNotContain("ollama-key", "2000", "jajka", "Ja");
    }

    @Test
    void shouldIncludeRoleGoalCaloriesAndProductNamesAndOmitPersonalDataFromThePrompt() {
        final ChatClient.ChatClientRequestSpec spec = mock(ChatClient.ChatClientRequestSpec.class);
        final ChatClient.CallResponseSpec call = mock(ChatClient.CallResponseSpec.class);
        final ChatClient chatClient = mock(ChatClient.class);
        when(chatClient.prompt()).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenReturn(call);
        when(call.entity(eq(WeekPlan.class))).thenReturn(
                acceptedWeek(List.of(new PlanPerson("Ja", Goal.LOSE_WEIGHT, 2000))));
        final DietGenerator generator = generator(chatClient, "ollama-key");

        generator.generate(List.of(new PlanPerson("Ja", Goal.LOSE_WEIGHT, 2000)), List.of("jajka"), List.of("orzechy"));

        final ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(spec).user(prompt.capture());
        verify(call, times(1)).entity(eq(WeekPlan.class));
        assertThat(prompt.getValue()).contains("dietitian", "seven consecutive days", "weight loss", "calorie deficit",
                "2000", "within ten percent", "Breakfast, lunch, and dinner names must be unique", "without a break",
                "summed across the seven days", "calendar dates", "jajka", "orzechy", "ready-made shop item",
                "preferences only", "user's language as typed", "proteinG", "carbsG", "fatG", "amount",
                "two or three short sentences", "temperature", "time", "oven dish", "Ja", "personName");
        assertThat(prompt.getValue()).doesNotContain("alice", "34", "180", "82.5", "MALE", "LOSE_WEIGHT", "MODERATE",
                "alice@example.com", "email", "height", "activity", "login");
    }

    @Test
    void shouldNameEachSelectedPersonGoalAndCaloriesInOrderAndOmitBodyAndEmail() {
        final List<PlanPerson> people = List.of(
                new PlanPerson("Ja", Goal.MAINTAIN, 2000),
                new PlanPerson("Anna", Goal.LOSE_WEIGHT, 795));
        final ChatClient.ChatClientRequestSpec spec = mock(ChatClient.ChatClientRequestSpec.class);
        final ChatClient.CallResponseSpec call = mock(ChatClient.CallResponseSpec.class);
        final ChatClient chatClient = mock(ChatClient.class);
        when(chatClient.prompt()).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenReturn(call);
        when(call.entity(eq(WeekPlan.class))).thenReturn(acceptedWeek(people));
        final DietGenerator generator = generator(chatClient, "ollama-key");

        final PlanResult actual = generator.generate(people, List.of("jajka"), List.of("orzechy"));

        final ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(spec).user(prompt.capture());
        verify(call, times(1)).entity(eq(WeekPlan.class));
        assertThat(prompt.getValue()).containsSubsequence(
                "Ja", "weight maintenance", "2000", "Anna", "weight loss", "calorie deficit", "795");
        assertThat(prompt.getValue()).doesNotContain(
                "alice", "34", "180", "82.5", "MALE", "FEMALE", "LOSE_WEIGHT", "MAINTAIN", "MODERATE", "SEDENTARY",
                "alice@example.com", "email", "height", "activity", "login");
        final PlanResult expected = PlanResult.success(acceptedWeek(people), people);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    private DietGenerator generator(final ChatClient chatClient, final String apiKey) {
        final ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(chatClient);
        return new DietGenerator(builder, new WeekPlanValidator(), apiKey);
    }

    private ChatClient chatClientReturning(final WeekPlan weekPlan) {
        return chatClientReturning(weekPlan, mock(ChatClient.CallResponseSpec.class));
    }

    private ChatClient chatClientReturning(final WeekPlan weekPlan, final ChatClient.CallResponseSpec call) {
        final ChatClient.ChatClientRequestSpec spec = mock(ChatClient.ChatClientRequestSpec.class);
        final ChatClient chatClient = mock(ChatClient.class);
        when(chatClient.prompt()).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenReturn(call);
        when(call.entity(eq(WeekPlan.class))).thenReturn(weekPlan);
        return chatClient;
    }

    private WeekPlan acceptedWeek(final List<PlanPerson> people) {
        return new WeekPlan(IntStream.range(0, 7)
                .mapToObj((final int index) -> new DayMeals(
                        meal("Sniadanie " + index, people, true),
                        meal("Jogurt", people, false),
                        meal("Obiad " + index, people, false),
                        meal("Kolacja " + index, people, false)))
                .toList(), List.of(new ShoppingItem("jogurt", "7 szt.")));
    }

    private WeekPlan rejectedWeek() {
        final List<PlanPerson> people = List.of(new PlanPerson("Ja", Goal.MAINTAIN, 2000));
        return new WeekPlan(IntStream.range(0, 7)
                .mapToObj((final int index) -> new DayMeals(
                        meal("Jajecznica", people, true),
                        meal("Jogurt", people, false),
                        meal("Obiad " + index, people, false),
                        meal("Kolacja " + index, people, false)))
                .toList(), List.of());
    }

    private Meal meal(final String name, final List<PlanPerson> people, final boolean carriesTarget) {
        return new Meal(name, "", people.stream()
                .map((final PlanPerson person) -> new Portion(
                        person.name(), List.of(), carriesTarget ? person.kcal() : 0, 0, 0, 0))
                .toList());
    }
}
