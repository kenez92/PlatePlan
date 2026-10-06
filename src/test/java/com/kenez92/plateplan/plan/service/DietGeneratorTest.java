package com.kenez92.plateplan.plan.service;

import java.util.List;

import com.kenez92.plateplan.plan.model.DietPlan;
import com.kenez92.plateplan.plan.model.Meal;
import com.kenez92.plateplan.plan.model.PlanResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
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

        final PlanResult actual = generator.generate(2000, List.of("jajka"), List.of("orzechy"));

        final PlanResult expected = PlanResult.success(expectedPlan);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnUnavailableWhenTheApiKeyIsMissing() {
        final ChatClient chatClient = mock(ChatClient.class);
        final DietGenerator generator = generator(chatClient, "");

        final PlanResult actual = generator.generate(2000, List.of("jajka"), List.of("orzechy"));

        verify(chatClient, never()).prompt();
        final PlanResult expected = PlanResult.unavailable();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnUnavailableWhenTheModelCallFails() {
        final ChatClient.ChatClientRequestSpec spec = mock(ChatClient.ChatClientRequestSpec.class);
        final ChatClient chatClient = mock(ChatClient.class);
        when(chatClient.prompt()).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenThrow(new RuntimeException("The model is unreachable"));
        final DietGenerator generator = generator(chatClient, "ollama-key");

        final PlanResult actual = generator.generate(2000, List.of("jajka"), List.of("orzechy"));

        final PlanResult expected = PlanResult.unavailable();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldIncludeCaloriesAndProductNamesAndOmitPersonalDataFromThePrompt() {
        final ChatClient.ChatClientRequestSpec spec = mock(ChatClient.ChatClientRequestSpec.class);
        final ChatClient.CallResponseSpec call = mock(ChatClient.CallResponseSpec.class);
        final ChatClient chatClient = mock(ChatClient.class);
        when(chatClient.prompt()).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenReturn(call);
        when(call.entity(eq(DietPlan.class))).thenReturn(samplePlan());
        final DietGenerator generator = generator(chatClient, "ollama-key");

        generator.generate(2000, List.of("jajka"), List.of("orzechy"));

        final ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(spec).user(prompt.capture());
        assertThat(prompt.getValue()).contains("2000", "jajka", "orzechy",
                "ready-made shop item", "preferences only", "user's language as typed");
        assertThat(prompt.getValue()).doesNotContain("alice", "34", "180", "82.5", "MALE");
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
                new Meal("Jajecznica", List.of("jajka", "chleb"), 450),
                new Meal("Jogurt", List.of("jogurt", "banan"), 250),
                new Meal("Schabowy", List.of("schab", "ziemniaki"), 800),
                new Meal("Zupa", List.of("warzywa"), 500),
                List.of("jajka", "chleb", "jogurt", "banan", "schab", "ziemniaki", "warzywa"));
    }
}
