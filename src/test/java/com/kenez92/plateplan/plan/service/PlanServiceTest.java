package com.kenez92.plateplan.plan.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.kenez92.plateplan.plan.model.DietPlan;
import com.kenez92.plateplan.plan.model.Meal;
import com.kenez92.plateplan.plan.model.PlanResult;
import com.kenez92.plateplan.profile.db.UserProfile;
import com.kenez92.plateplan.profile.db.UserProfileRepository;
import com.kenez92.plateplan.profile.format.ProductListFormat;
import com.kenez92.plateplan.profile.model.ActivityLevel;
import com.kenez92.plateplan.profile.model.Goal;
import com.kenez92.plateplan.profile.model.Sex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Test
    void shouldReturnTheDietPlanWhenTheProfileAndCaloriesArePresent() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final DietGenerator dietGenerator = mock(DietGenerator.class);
        final PlanService service = service(repository, dietGenerator);
        final DietPlan dietPlan = samplePlan();
        when(repository.findById("alice")).thenReturn(Optional.of(storedRow(2000)));
        when(dietGenerator.generate(2000, List.of("jajka"), List.of("orzechy")))
                .thenReturn(PlanResult.success(dietPlan, 2000));

        final PlanResult actual = service.generate("alice");

        final PlanResult expected = PlanResult.success(dietPlan, 2000);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnNoProfileWhenThereIsNoRow() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final DietGenerator dietGenerator = mock(DietGenerator.class);
        final PlanService service = service(repository, dietGenerator);
        when(repository.findById("alice")).thenReturn(Optional.empty());

        final PlanResult actual = service.generate("alice");

        verify(dietGenerator, never()).generate(anyInt(), any(), any());
        final PlanResult expected = PlanResult.noProfile();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnNoCaloriesWhenTheStoredTargetIsMissing() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final DietGenerator dietGenerator = mock(DietGenerator.class);
        final PlanService service = service(repository, dietGenerator);
        when(repository.findById("alice")).thenReturn(Optional.of(storedRow(null)));

        final PlanResult actual = service.generate("alice");

        verify(dietGenerator, never()).generate(anyInt(), any(), any());
        final PlanResult expected = PlanResult.noCalories();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnUnavailableWhenTheDatabaseIsDown() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final DietGenerator dietGenerator = mock(DietGenerator.class);
        final PlanService service = service(repository, dietGenerator);
        when(repository.findById("alice"))
                .thenThrow(new DataAccessResourceFailureException("The database is unreachable"));

        final PlanResult actual = service.generate("alice");

        verify(dietGenerator, never()).generate(anyInt(), any(), any());
        final PlanResult expected = PlanResult.unavailable();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnUnavailableWhenTheGeneratorIsUnavailable() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final DietGenerator dietGenerator = mock(DietGenerator.class);
        final PlanService service = service(repository, dietGenerator);
        when(repository.findById("alice")).thenReturn(Optional.of(storedRow(2000)));
        when(dietGenerator.generate(2000, List.of("jajka"), List.of("orzechy")))
                .thenReturn(PlanResult.unavailable());

        final PlanResult actual = service.generate("alice");

        final PlanResult expected = PlanResult.unavailable();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    private PlanService service(final UserProfileRepository repository, final DietGenerator dietGenerator) {
        return new PlanService(repository, new ProductListFormat(), dietGenerator);
    }

    private UserProfile storedRow(final Integer confirmedCalories) {
        return new UserProfile("alice", 34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN,
                ActivityLevel.MODERATE, confirmedCalories, "jajka", "orzechy");
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
