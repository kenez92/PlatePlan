package com.kenez92.plateplan.plan.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.kenez92.plateplan.plan.model.DayMeals;
import com.kenez92.plateplan.plan.model.Ingredient;
import com.kenez92.plateplan.plan.model.Meal;
import com.kenez92.plateplan.plan.model.PlanOption;
import com.kenez92.plateplan.plan.model.PlanPerson;
import com.kenez92.plateplan.plan.model.PlanResult;
import com.kenez92.plateplan.plan.model.Portion;
import com.kenez92.plateplan.plan.model.ShoppingItem;
import com.kenez92.plateplan.plan.model.WeekPlan;
import com.kenez92.plateplan.profile.db.HouseholdMember;
import com.kenez92.plateplan.profile.db.HouseholdMemberRepository;
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
import org.springframework.transaction.CannotCreateTransactionException;

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
    void shouldReturnTheWeekWhenTheProfileAndCaloriesArePresent() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final DietGenerator dietGenerator = mock(DietGenerator.class);
        final PlanService service = service(repository, dietGenerator);
        final WeekPlan weekPlan = samplePlan();
        when(repository.findById("alice")).thenReturn(Optional.of(storedRow(2000)));
        when(dietGenerator.generate(
                List.of(new PlanPerson("Ja", Goal.MAINTAIN, 2000)), List.of("jajka"), List.of("orzechy")))
                .thenReturn(PlanResult.success(weekPlan, List.of(new PlanPerson("Ja", Goal.MAINTAIN, 2000))));

        final PlanResult actual = service.generate("alice", List.of("owner"));

        verify(repository, never()).save(any());
        verify(repository, never()).replaceConfirmedCalories(any(), anyInt());
        verify(repository, never()).replaceBodyAndProducts(any(), anyInt(), anyInt(), any(), any(), any(), any(), any(),
                any(), any(), any());
        verify(repository, never()).replaceBodyProductsAndCalories(any(), anyInt(), anyInt(), any(), any(), any(),
                any(), anyInt(), any(), any(), any(), any());
        final PlanResult expected = PlanResult.success(weekPlan, List.of(new PlanPerson("Ja", Goal.MAINTAIN, 2000)));
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnNoProfileWhenThereIsNoRow() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final DietGenerator dietGenerator = mock(DietGenerator.class);
        final PlanService service = service(repository, dietGenerator);
        when(repository.findById("alice")).thenReturn(Optional.empty());

        final PlanResult actual = service.generate("alice", List.of("owner"));

        verify(dietGenerator, never()).generate(any(), any(), any());
        final PlanResult expected = PlanResult.noProfile();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnUnavailableWhenTheTransactionCannotStart() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final DietGenerator dietGenerator = mock(DietGenerator.class);
        final PlanService service = service(repository, dietGenerator);
        when(repository.findById("alice"))
                .thenThrow(new CannotCreateTransactionException("The database is unreachable"));

        final PlanResult actual = service.generate("alice", List.of("owner"));

        verify(dietGenerator, never()).generate(any(), any(), any());
        final PlanResult expected = PlanResult.unavailable();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnNoCaloriesWhenTheStoredTargetIsMissing() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final DietGenerator dietGenerator = mock(DietGenerator.class);
        final PlanService service = service(repository, dietGenerator);
        when(repository.findById("alice")).thenReturn(Optional.of(storedRow(null)));

        final PlanResult actual = service.generate("alice", List.of("owner"));

        verify(dietGenerator, never()).generate(any(), any(), any());
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

        final PlanResult actual = service.generate("alice", List.of("owner"));

        verify(dietGenerator, never()).generate(any(), any(), any());
        final PlanResult expected = PlanResult.unavailable();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnUnavailableWhenLoadingMembersFails() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final DietGenerator dietGenerator = mock(DietGenerator.class);
        final PlanService service = service(repository, members, dietGenerator);
        when(repository.findById("alice")).thenReturn(Optional.of(storedRow(2000)));
        when(members.findByLoginOrderByMemberIdAsc("alice"))
                .thenThrow(new DataAccessResourceFailureException("The database is unreachable"));

        final PlanResult actual = service.generate("alice", List.of("owner"));

        verify(dietGenerator, never()).generate(any(), any(), any());
        final PlanResult expected = PlanResult.unavailable();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnUnavailableWhenTheGeneratorIsUnavailable() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final DietGenerator dietGenerator = mock(DietGenerator.class);
        final PlanService service = service(repository, dietGenerator);
        when(repository.findById("alice")).thenReturn(Optional.of(storedRow(2000)));
        when(dietGenerator.generate(
                List.of(new PlanPerson("Ja", Goal.MAINTAIN, 2000)), List.of("jajka"), List.of("orzechy")))
                .thenReturn(PlanResult.unavailable());

        final PlanResult actual = service.generate("alice", List.of("owner"));

        final PlanResult expected = PlanResult.unavailable();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldPassTheStoredGoalToTheGenerator() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final DietGenerator dietGenerator = mock(DietGenerator.class);
        final PlanService service = service(repository, dietGenerator);
        final WeekPlan weekPlan = samplePlan();
        when(repository.findById("alice")).thenReturn(Optional.of(storedRow(2000, Goal.LOSE_WEIGHT)));
        when(dietGenerator.generate(
                List.of(new PlanPerson("Ja", Goal.LOSE_WEIGHT, 2000)), List.of("jajka"), List.of("orzechy")))
                .thenReturn(PlanResult.success(weekPlan, List.of(new PlanPerson("Ja", Goal.LOSE_WEIGHT, 2000))));

        final PlanResult actual = service.generate("alice", List.of("owner"));

        final PlanResult expected = PlanResult.success(weekPlan, List.of(new PlanPerson("Ja", Goal.LOSE_WEIGHT, 2000)));
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldSendTheOwnersStoredName() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final DietGenerator dietGenerator = mock(DietGenerator.class);
        final PlanService service = service(repository, dietGenerator);
        final WeekPlan weekPlan = samplePlan();
        when(repository.findById("alice")).thenReturn(Optional.of(
                storedRow(2000, Goal.MAINTAIN, "Daniel", "jajka", "orzechy")));
        when(dietGenerator.generate(
                List.of(new PlanPerson("Daniel", Goal.MAINTAIN, 2000)), List.of("jajka"), List.of("orzechy")))
                .thenReturn(PlanResult.success(weekPlan, List.of(new PlanPerson("Daniel", Goal.MAINTAIN, 2000))));

        final PlanResult actual = service.generate("alice", List.of("owner"));

        final PlanResult expected = PlanResult.success(weekPlan, List.of(new PlanPerson("Daniel", Goal.MAINTAIN, 2000)));
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnNoSelectionWhenTheCallerSelectsNobody() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final DietGenerator dietGenerator = mock(DietGenerator.class);
        final PlanService service = service(repository, members, dietGenerator);

        final PlanResult actual = service.generate("alice", List.of());

        verify(repository, never()).findById(any());
        verify(members, never()).findByLoginOrderByMemberIdAsc(any());
        verify(dietGenerator, never()).generate(any(), any(), any());
        final PlanResult expected = PlanResult.noSelection();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnNoCaloriesWhenASelectedPersonHasNoTarget() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final DietGenerator dietGenerator = mock(DietGenerator.class);
        final PlanService service = service(repository, members, dietGenerator);
        when(repository.findById("alice")).thenReturn(Optional.of(storedRow(2000)));
        when(members.findByLoginOrderByMemberIdAsc("alice")).thenReturn(List.of(
                member(4L, "Anna", Goal.LOSE_WEIGHT, null, "chleb", "gluten")));

        final PlanResult actual = service.generate("alice", List.of("owner", "4"));

        verify(dietGenerator, never()).generate(any(), any(), any());
        final PlanResult expected = PlanResult.noCalories();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldLeaveOutAnUnselectedPersonsExclusion() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final DietGenerator dietGenerator = mock(DietGenerator.class);
        final PlanService service = service(repository, members, dietGenerator);
        final WeekPlan weekPlan = samplePlan();
        when(repository.findById("alice")).thenReturn(Optional.of(storedRow(2000)));
        when(members.findByLoginOrderByMemberIdAsc("alice")).thenReturn(List.of(
                member(4L, "Anna", Goal.LOSE_WEIGHT, null, "chleb", "jajka")));
        when(dietGenerator.generate(
                List.of(new PlanPerson("Ja", Goal.MAINTAIN, 2000)), List.of("jajka"), List.of("orzechy")))
                .thenReturn(PlanResult.success(weekPlan, List.of(new PlanPerson("Ja", Goal.MAINTAIN, 2000))));

        final PlanResult actual = service.generate("alice", List.of("owner"));

        final PlanResult expected = PlanResult.success(weekPlan, List.of(new PlanPerson("Ja", Goal.MAINTAIN, 2000)));
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldGenerateForASelectedMemberWhenTheUnselectedOwnerHasNoCalories() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final DietGenerator dietGenerator = mock(DietGenerator.class);
        final PlanService service = service(repository, members, dietGenerator);
        final WeekPlan weekPlan = samplePlan();
        when(repository.findById("alice")).thenReturn(Optional.of(storedRow(null)));
        when(members.findByLoginOrderByMemberIdAsc("alice")).thenReturn(List.of(
                member(4L, "Anna", Goal.LOSE_WEIGHT, 795, "chleb", "orzechy")));
        when(dietGenerator.generate(
                List.of(new PlanPerson("Anna", Goal.LOSE_WEIGHT, 795)), List.of("chleb"), List.of("orzechy")))
                .thenReturn(PlanResult.success(weekPlan, List.of(new PlanPerson("Anna", Goal.LOSE_WEIGHT, 795))));

        final PlanResult actual = service.generate("alice", List.of("4"));

        final PlanResult expected = PlanResult.success(weekPlan, List.of(new PlanPerson("Anna", Goal.LOSE_WEIGHT, 795)));
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldMergeOnlyTheSelectedPeopleKeepingTheFirstSpellingAndHouseholdOrder() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final DietGenerator dietGenerator = mock(DietGenerator.class);
        final PlanService service = service(repository, members, dietGenerator);
        final WeekPlan weekPlan = samplePlan();
        when(repository.findById("alice")).thenReturn(Optional.of(
                storedRow(2000, Goal.MAINTAIN, null, "Ser;Mleko", "Orzechy")));
        when(members.findByLoginOrderByMemberIdAsc("alice")).thenReturn(List.of(
                member(4L, "Anna", Goal.GAIN, 795, "ser;Chleb;orzechy", "mleko"),
                member(9L, "Bartek", Goal.LOSE_WEIGHT, 1800, null, null),
                member(12L, "Ola", Goal.MAINTAIN, null, "Ryz", "Gluten")));
        when(dietGenerator.generate(
                List.of(
                        new PlanPerson("Ja", Goal.MAINTAIN, 2000),
                        new PlanPerson("Anna", Goal.GAIN, 795),
                        new PlanPerson("Bartek", Goal.LOSE_WEIGHT, 1800)),
                List.of("Ser", "Chleb"),
                List.of("Orzechy", "mleko")))
                .thenReturn(PlanResult.success(weekPlan, List.of(
                        new PlanPerson("Ja", Goal.MAINTAIN, 2000),
                        new PlanPerson("Anna", Goal.GAIN, 795),
                        new PlanPerson("Bartek", Goal.LOSE_WEIGHT, 1800))));

        final PlanResult actual = service.generate("alice", List.of("9", "4", "owner"));

        final PlanResult expected = PlanResult.success(weekPlan, List.of(
                new PlanPerson("Ja", Goal.MAINTAIN, 2000),
                new PlanPerson("Anna", Goal.GAIN, 795),
                new PlanPerson("Bartek", Goal.LOSE_WEIGHT, 1800)));
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnNoSelectionWhenATokenMatchesNobody() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final DietGenerator dietGenerator = mock(DietGenerator.class);
        final PlanService service = service(repository, members, dietGenerator);
        when(repository.findById("alice")).thenReturn(Optional.of(storedRow(2000)));
        when(members.findByLoginOrderByMemberIdAsc("alice")).thenReturn(List.of(
                member(4L, "Anna", Goal.LOSE_WEIGHT, null, "chleb", "gluten")));

        final PlanResult actual = service.generate("alice", List.of("owner", "15"));

        verify(dietGenerator, never()).generate(any(), any(), any());
        final PlanResult expected = PlanResult.noSelection();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldGenerateForEverySavedPersonWhenNoSelectionIsPassed() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final DietGenerator dietGenerator = mock(DietGenerator.class);
        final PlanService service = service(repository, members, dietGenerator);
        final WeekPlan weekPlan = samplePlan();
        final List<PlanPerson> people = List.of(
                new PlanPerson("Ja", Goal.MAINTAIN, 2000),
                new PlanPerson("Anna", Goal.LOSE_WEIGHT, 795));
        when(repository.findById("alice")).thenReturn(Optional.of(storedRow(2000)));
        when(members.findByLoginOrderByMemberIdAsc("alice")).thenReturn(List.of(
                member(4L, "Anna", Goal.LOSE_WEIGHT, 795, "chleb", "gluten")));
        when(dietGenerator.generate(people, List.of("jajka", "chleb"), List.of("orzechy", "gluten")))
                .thenReturn(PlanResult.success(weekPlan, people));

        final PlanResult actual = service.generate("alice", null);

        final PlanResult expected = PlanResult.success(weekPlan, people);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldListTheOwnerThenMembersAndUseJaWhenTheOwnerNameIsBlank() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final PlanService service = service(repository, members, mock(DietGenerator.class));
        when(repository.findById("alice")).thenReturn(Optional.of(
                storedRow(2000, Goal.MAINTAIN, "  ", "jajka", "orzechy")));
        when(members.findByLoginOrderByMemberIdAsc("alice")).thenReturn(List.of(
                member(4L, "Anna", Goal.GAIN, 795, null, null),
                member(9L, "Bartek", Goal.LOSE_WEIGHT, 1800, null, null)));

        final List<PlanOption> actual = service.options("alice");

        final List<PlanOption> expected = List.of(
                new PlanOption("owner", "Ja"),
                new PlanOption("4", "Anna"),
                new PlanOption("9", "Bartek"));
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    private PlanService service(final UserProfileRepository repository, final DietGenerator dietGenerator) {
        return service(repository, mock(HouseholdMemberRepository.class), dietGenerator);
    }

    private PlanService service(final UserProfileRepository repository,
                                final HouseholdMemberRepository members,
                                final DietGenerator dietGenerator) {
        return new PlanService(repository, members, new ProductListFormat(), dietGenerator);
    }

    private UserProfile storedRow(final Integer confirmedCalories) {
        return storedRow(confirmedCalories, Goal.MAINTAIN);
    }

    private UserProfile storedRow(final Integer confirmedCalories, final Goal goal) {
        return storedRow(confirmedCalories, goal, null, "jajka", "orzechy");
    }

    private UserProfile storedRow(final Integer confirmedCalories,
                                  final Goal goal,
                                  final String displayName,
                                  final String preferred,
                                  final String excluded) {
        return new UserProfile("alice", 34, 180, new BigDecimal("82.5"), Sex.MALE, goal,
                ActivityLevel.MODERATE, confirmedCalories, preferred, excluded, "alice@example.com", displayName);
    }

    private HouseholdMember member(final long memberId,
                                   final String name,
                                   final Goal goal,
                                   final Integer kcal,
                                   final String preferred,
                                   final String excluded) {
        return new HouseholdMember(memberId, "alice", name, 4, 100, new BigDecimal("18.0"), Sex.FEMALE, goal,
                ActivityLevel.LIGHT, kcal, preferred, excluded);
    }

    private WeekPlan samplePlan() {
        return new WeekPlan(List.of(new DayMeals(
                meal("Jajecznica", List.of(new Ingredient("jajka", "2 szt."), new Ingredient("chleb", "60 g")),
                        450, 28, 32, 22, "Rozbij jajka i usmaż na patelni."),
                meal("Jogurt", List.of(new Ingredient("jogurt", "200 g"), new Ingredient("banan", "1 szt.")),
                        250, 18, 30, 4, "Otwórz jogurt i dodaj banana."),
                meal("Schabowy", List.of(new Ingredient("schab", "150 g"), new Ingredient("ziemniaki", "200 g")),
                        800, 40, 55, 35, "Usmaż schab i ugotuj ziemniaki."),
                meal("Zupa", List.of(new Ingredient("warzywa", "300 g")), 500, 12, 40, 18,
                        "Gotuj warzywa w wodzie do miękkości."))),
                List.of(
                        new ShoppingItem("jajka", "2 szt."),
                        new ShoppingItem("chleb", "60 g"),
                        new ShoppingItem("jogurt", "200 g"),
                        new ShoppingItem("banan", "1 szt."),
                        new ShoppingItem("schab", "150 g"),
                        new ShoppingItem("ziemniaki", "200 g"),
                        new ShoppingItem("warzywa", "300 g")));
    }

    private Meal meal(final String name,
                      final List<Ingredient> ingredients,
                      final int kcal,
                      final int protein,
                      final int carbs,
                      final int fat,
                      final String preparation) {
        return new Meal(name, preparation, List.of(new Portion("Ja", ingredients, kcal, protein, carbs, fat)));
    }
}
