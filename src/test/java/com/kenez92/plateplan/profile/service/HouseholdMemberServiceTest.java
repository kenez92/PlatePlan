package com.kenez92.plateplan.profile.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.kenez92.plateplan.profile.controller.dto.HouseholdMemberForm;
import com.kenez92.plateplan.profile.db.HouseholdMember;
import com.kenez92.plateplan.profile.db.HouseholdMemberRepository;
import com.kenez92.plateplan.profile.db.UserProfile;
import com.kenez92.plateplan.profile.db.UserProfileRepository;
import com.kenez92.plateplan.profile.format.ProductListFormat;
import com.kenez92.plateplan.profile.model.ActivityLevel;
import com.kenez92.plateplan.profile.model.Goal;
import com.kenez92.plateplan.profile.model.HouseholdMemberError;
import com.kenez92.plateplan.profile.model.HouseholdMemberResult;
import com.kenez92.plateplan.profile.model.Sex;
import com.kenez92.plateplan.profile.validator.DisplayNameValidator;
import com.kenez92.plateplan.profile.validator.PersonBoundsValidator;
import com.kenez92.plateplan.profile.validator.ProductListsValidator;
import com.kenez92.plateplan.profile.validator.ProductNameValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HouseholdMemberServiceTest {

    @Test
    void shouldStoreAge2AndWeight8() {
        final UserProfileRepository profiles = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final HouseholdMemberService service = service(profiles, members);
        final UserProfile owner = owner("Daniel");
        final ArgumentCaptor<HouseholdMember> saved = ArgumentCaptor.forClass(HouseholdMember.class);
        when(profiles.findById("alice")).thenReturn(Optional.of(owner));
        when(members.findByLoginOrderByMemberIdAsc("alice")).thenReturn(List.of());
        when(members.save(any(HouseholdMember.class))).then(returnsFirstArg());
        final HouseholdMemberForm form = form("Ada", 2, 70, new BigDecimal("8.0"), Sex.FEMALE, Goal.MAINTAIN,
                ActivityLevel.SEDENTARY);

        final HouseholdMemberResult actual = service.create("alice", form);

        verify(members).save(saved.capture());
        verifyOwnerUntouched(profiles);
        final HouseholdMember expectedMember = new HouseholdMember("alice", "Ada", 2, 70, new BigDecimal("8.0"),
                Sex.FEMALE, Goal.MAINTAIN, ActivityLevel.SEDENTARY, 416, null, null);
        final UserProfile expectedOwner = owner("Daniel");
        assertThat(owner).usingRecursiveComparison().isEqualTo(expectedOwner);
        assertThat(saved.getValue()).usingRecursiveComparison().isEqualTo(expectedMember);
        assertThat(actual).usingRecursiveComparison().isEqualTo(HouseholdMemberResult.saved(expectedMember));
    }

    @Test
    void shouldRefuseAgeBelow2() {
        final UserProfileRepository profiles = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final HouseholdMemberService service = service(profiles, members);
        when(profiles.findById("alice")).thenReturn(Optional.of(owner(null)));
        final HouseholdMemberForm form = form("Ada", 1, 70, new BigDecimal("8.0"), Sex.FEMALE, Goal.MAINTAIN,
                ActivityLevel.SEDENTARY);

        final HouseholdMemberResult actual = service.create("alice", form);

        verify(members, never()).save(any());
        verifyOwnerUntouched(profiles);
        assertThat(actual).usingRecursiveComparison().isEqualTo(HouseholdMemberResult.rejected(HouseholdMemberError.BODY));
    }

    @Test
    void shouldRefuseWeightBelow8() {
        final UserProfileRepository profiles = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final HouseholdMemberService service = service(profiles, members);
        when(profiles.findById("alice")).thenReturn(Optional.of(owner(null)));
        final HouseholdMemberForm form = form("Ada", 2, 70, new BigDecimal("7.9"), Sex.FEMALE, Goal.MAINTAIN,
                ActivityLevel.SEDENTARY);

        final HouseholdMemberResult actual = service.create("alice", form);

        verify(members, never()).save(any());
        assertThat(actual).usingRecursiveComparison().isEqualTo(HouseholdMemberResult.rejected(HouseholdMemberError.BODY));
    }

    @Test
    void shouldStoreFormula795() {
        final UserProfileRepository profiles = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final HouseholdMemberService service = service(profiles, members);
        final ArgumentCaptor<HouseholdMember> saved = ArgumentCaptor.forClass(HouseholdMember.class);
        when(profiles.findById("alice")).thenReturn(Optional.of(owner("Daniel")));
        when(members.findByLoginOrderByMemberIdAsc("alice")).thenReturn(List.of());
        when(members.save(any(HouseholdMember.class))).then(returnsFirstArg());
        final HouseholdMemberForm form = form("Ada", 2, 70, new BigDecimal("8.5"), Sex.FEMALE, Goal.GAIN,
                ActivityLevel.MODERATE);

        final HouseholdMemberResult actual = service.create("alice", form);

        verify(members).save(saved.capture());
        final HouseholdMember expectedMember = new HouseholdMember("alice", "Ada", 2, 70, new BigDecimal("8.5"),
                Sex.FEMALE, Goal.GAIN, ActivityLevel.MODERATE, 795, null, null);
        assertThat(saved.getValue()).usingRecursiveComparison().isEqualTo(expectedMember);
        assertThat(actual).usingRecursiveComparison().isEqualTo(HouseholdMemberResult.saved(expectedMember));
    }

    @Test
    void shouldRefuseFormula166WithoutCreatingARow() {
        final UserProfileRepository profiles = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final HouseholdMemberService service = service(profiles, members);
        when(profiles.findById("alice")).thenReturn(Optional.of(owner("Daniel")));
        when(members.findByLoginOrderByMemberIdAsc("alice")).thenReturn(List.of());
        final HouseholdMemberForm form = form("Ada", 2, 70, new BigDecimal("8.0"), Sex.FEMALE, Goal.LOSE_WEIGHT,
                ActivityLevel.SEDENTARY);

        final HouseholdMemberResult actual = service.create("alice", form);

        verify(members, never()).save(any());
        verifyOwnerUntouched(profiles);
        assertThat(actual).usingRecursiveComparison()
                .isEqualTo(HouseholdMemberResult.rejected(HouseholdMemberError.CALORIES));
    }

    @Test
    void shouldRefuseFormula9837WithoutCreatingARow() {
        final UserProfileRepository profiles = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final HouseholdMemberService service = service(profiles, members);
        when(profiles.findById("alice")).thenReturn(Optional.of(owner("Daniel")));
        when(members.findByLoginOrderByMemberIdAsc("alice")).thenReturn(List.of());
        final HouseholdMemberForm form = form("Ada", 2, 250, new BigDecimal("400.0"), Sex.MALE, Goal.GAIN,
                ActivityLevel.HIGH);

        final HouseholdMemberResult actual = service.create("alice", form);

        verify(members, never()).save(any());
        assertThat(actual).usingRecursiveComparison()
                .isEqualTo(HouseholdMemberResult.rejected(HouseholdMemberError.CALORIES));
    }

    @Test
    void shouldRefuseJaWhenTheOwnerHasNoName() {
        final UserProfileRepository profiles = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final HouseholdMemberService service = service(profiles, members);
        final UserProfile owner = owner(null);
        when(profiles.findById("alice")).thenReturn(Optional.of(owner));
        when(members.findByLoginOrderByMemberIdAsc("alice")).thenReturn(List.of());
        final HouseholdMemberForm form = form("ja", 34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN,
                ActivityLevel.MODERATE);

        final HouseholdMemberResult actual = service.create("alice", form);

        verify(members, never()).save(any());
        verifyOwnerUntouched(profiles);
        assertThat(owner).usingRecursiveComparison().isEqualTo(owner(null));
        assertThat(actual).usingRecursiveComparison()
                .isEqualTo(HouseholdMemberResult.rejected(HouseholdMemberError.NAME_TAKEN));
    }

    @Test
    void shouldStoreJaWhenTheOwnerIsNamedDaniel() {
        final UserProfileRepository profiles = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final HouseholdMemberService service = service(profiles, members);
        final ArgumentCaptor<HouseholdMember> saved = ArgumentCaptor.forClass(HouseholdMember.class);
        when(profiles.findById("alice")).thenReturn(Optional.of(owner("Daniel")));
        when(members.findByLoginOrderByMemberIdAsc("alice")).thenReturn(List.of());
        when(members.save(any(HouseholdMember.class))).then(returnsFirstArg());
        final HouseholdMemberForm form = form("Ja", 34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN,
                ActivityLevel.MODERATE);

        final HouseholdMemberResult actual = service.create("alice", form);

        verify(members).save(saved.capture());
        final HouseholdMember expectedMember = new HouseholdMember("alice", "Ja", 34, 180, new BigDecimal("82.5"),
                Sex.MALE, Goal.MAINTAIN, ActivityLevel.MODERATE, 2767, null, null);
        assertThat(saved.getValue()).usingRecursiveComparison().isEqualTo(expectedMember);
        assertThat(actual).usingRecursiveComparison().isEqualTo(HouseholdMemberResult.saved(expectedMember));
    }

    @Test
    void shouldTreatAnnaAndAnnaAsOneName() {
        final UserProfileRepository profiles = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final HouseholdMemberService service = service(profiles, members);
        final HouseholdMember existing = stored(1L, "Anna");
        when(profiles.findById("alice")).thenReturn(Optional.of(owner("Daniel")));
        when(members.findByLoginOrderByMemberIdAsc("alice")).thenReturn(List.of(existing));
        final HouseholdMemberForm form = form("anna", 34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN,
                ActivityLevel.MODERATE);

        final HouseholdMemberResult actual = service.create("alice", form);

        verify(members, never()).save(any());
        verify(members, never()).deleteByMemberIdAndLogin(anyLong(), any());
        final HouseholdMember unchanged = stored(1L, "Anna");
        assertThat(existing).usingRecursiveComparison().isEqualTo(unchanged);
        assertThat(actual).usingRecursiveComparison()
                .isEqualTo(HouseholdMemberResult.rejected(HouseholdMemberError.NAME_TAKEN));
    }

    @Test
    void shouldRefuseTheFourthMemberAndKeepTheThree() {
        final UserProfileRepository profiles = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final HouseholdMemberService service = service(profiles, members);
        final HouseholdMember first = stored(1L, "Anna");
        final HouseholdMember second = stored(2L, "Bartek");
        final HouseholdMember third = stored(3L, "Celina");
        when(profiles.findById("alice")).thenReturn(Optional.of(owner("Daniel")));
        when(members.findByLoginOrderByMemberIdAsc("alice")).thenReturn(List.of(third, first, second));
        final HouseholdMemberForm form = form("Dawid", 34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN,
                ActivityLevel.MODERATE);

        final HouseholdMemberResult created = service.create("alice", form);
        final HouseholdMemberResult listed = service.list("alice");

        verify(members, never()).save(any());
        verify(members, never()).deleteByMemberIdAndLogin(anyLong(), any());
        verifyOwnerUntouched(profiles);
        assertThat(created).usingRecursiveComparison().isEqualTo(HouseholdMemberResult.rejected(HouseholdMemberError.LIMIT));
        assertThat(listed).usingRecursiveComparison()
                .isEqualTo(HouseholdMemberResult.listed(List.of(first, second, third)));
    }

    @Test
    void shouldCreateNothingWhenTheOwnerRowIsMissing() {
        final UserProfileRepository profiles = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final HouseholdMemberService service = service(profiles, members);
        when(profiles.findById("alice")).thenReturn(Optional.empty());
        final HouseholdMemberForm form = form("Ada", 34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN,
                ActivityLevel.MODERATE);

        final HouseholdMemberResult actual = service.create("alice", form);

        verify(members, never()).save(any());
        verify(members, never()).findByLoginOrderByMemberIdAsc(any());
        assertThat(actual).usingRecursiveComparison()
                .isEqualTo(HouseholdMemberResult.rejected(HouseholdMemberError.NO_OWNER));
    }

    @Test
    void shouldKeepConfirmedCaloriesOnALaterBodySave() {
        final UserProfileRepository profiles = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final HouseholdMemberService service = service(profiles, members);
        final HouseholdMember current = new HouseholdMember(4L, "alice", "Ada", 2, 70, new BigDecimal("8.5"),
                Sex.FEMALE, Goal.GAIN, ActivityLevel.MODERATE, 795, null, null);
        when(profiles.findById("alice")).thenReturn(Optional.of(owner("Daniel")));
        when(members.findByLoginOrderByMemberIdAsc("alice")).thenReturn(List.of(current));
        when(members.replaceBody(4L, "alice", "Ada", 2, 70, new BigDecimal("8.0"), Sex.FEMALE, Goal.LOSE_WEIGHT,
                ActivityLevel.SEDENTARY, null, null)).thenReturn(1);
        final HouseholdMemberForm form = form("Ada", 2, 70, new BigDecimal("8.0"), Sex.FEMALE, Goal.LOSE_WEIGHT,
                ActivityLevel.SEDENTARY);

        final HouseholdMemberResult actual = service.replaceBody("alice", 4L, form);

        verify(members, never()).save(any());
        verifyOwnerUntouched(profiles);
        final HouseholdMember expectedMember = new HouseholdMember(4L, "alice", "Ada", 2, 70, new BigDecimal("8.0"),
                Sex.FEMALE, Goal.LOSE_WEIGHT, ActivityLevel.SEDENTARY, 795, null, null);
        assertThat(actual).usingRecursiveComparison().isEqualTo(HouseholdMemberResult.saved(expectedMember));
    }

    @Test
    void shouldDeleteOnlyTheRowOfThatLogin() {
        final UserProfileRepository profiles = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final HouseholdMemberService service = service(profiles, members);
        when(members.deleteByMemberIdAndLogin(4L, "alice")).thenReturn(1);

        final HouseholdMemberResult actual = service.delete("alice", 4L);

        verify(members).deleteByMemberIdAndLogin(4L, "alice");
        verify(members, never()).findById(any());
        verify(profiles, never()).findById(any());
        assertThat(actual).usingRecursiveComparison().isEqualTo(HouseholdMemberResult.deleted());
    }

    @Test
    void shouldDeleteNothingAndReturnNothingForAnotherAccountsId() {
        final UserProfileRepository profiles = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final HouseholdMemberService service = service(profiles, members);
        when(members.deleteByMemberIdAndLogin(9L, "alice")).thenReturn(0);

        final HouseholdMemberResult actual = service.delete("alice", 9L);

        verify(members, never()).findById(any());
        verify(members, never()).findAll();
        verify(profiles, never()).findById(any());
        assertThat(actual).usingRecursiveComparison()
                .isEqualTo(HouseholdMemberResult.rejected(HouseholdMemberError.NOT_FOUND));
    }

    @Test
    void shouldListMembersByMemberIdAscending() {
        final UserProfileRepository profiles = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final HouseholdMemberService service = service(profiles, members);
        final HouseholdMember first = stored(1L, "Anna");
        final HouseholdMember second = stored(2L, "Bartek");
        final HouseholdMember third = stored(3L, "Celina");
        when(members.findByLoginOrderByMemberIdAsc("alice")).thenReturn(List.of(third, first, second));

        final HouseholdMemberResult actual = service.list("alice");

        verify(members).findByLoginOrderByMemberIdAsc("alice");
        assertThat(actual).usingRecursiveComparison()
                .isEqualTo(HouseholdMemberResult.listed(List.of(first, second, third)));
    }

    @Test
    void shouldRefuseABlankMemberName() {
        final UserProfileRepository profiles = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final HouseholdMemberService service = service(profiles, members);
        when(profiles.findById("alice")).thenReturn(Optional.of(owner("Daniel")));
        final HouseholdMemberForm form = form("   ", 34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN,
                ActivityLevel.MODERATE);

        final HouseholdMemberResult actual = service.create("alice", form);

        verify(members, never()).save(any());
        assertThat(actual).usingRecursiveComparison().isEqualTo(HouseholdMemberResult.rejected(HouseholdMemberError.NAME));
    }

    @Test
    void shouldAllowASemicolonInAMemberName() {
        final UserProfileRepository profiles = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final HouseholdMemberService service = service(profiles, members);
        final ArgumentCaptor<HouseholdMember> saved = ArgumentCaptor.forClass(HouseholdMember.class);
        when(profiles.findById("alice")).thenReturn(Optional.of(owner("Daniel")));
        when(members.findByLoginOrderByMemberIdAsc("alice")).thenReturn(List.of());
        when(members.save(any(HouseholdMember.class))).then(returnsFirstArg());
        final HouseholdMemberForm form = form("A;B", 2, 70, new BigDecimal("8.0"), Sex.FEMALE, Goal.MAINTAIN,
                ActivityLevel.SEDENTARY);

        service.create("alice", form);

        verify(members).save(saved.capture());
        final HouseholdMember expectedMember = new HouseholdMember("alice", "A;B", 2, 70, new BigDecimal("8.0"),
                Sex.FEMALE, Goal.MAINTAIN, ActivityLevel.SEDENTARY, 416, null, null);
        assertThat(saved.getValue()).usingRecursiveComparison().isEqualTo(expectedMember);
    }

    @Test
    void shouldStoreNullWhenBothProductListsAreEmpty() {
        final UserProfileRepository profiles = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final HouseholdMemberService service = service(profiles, members);
        final ArgumentCaptor<HouseholdMember> saved = ArgumentCaptor.forClass(HouseholdMember.class);
        when(profiles.findById("alice")).thenReturn(Optional.of(owner("Daniel")));
        when(members.findByLoginOrderByMemberIdAsc("alice")).thenReturn(List.of());
        when(members.save(any(HouseholdMember.class))).then(returnsFirstArg());

        service.create("alice", new HouseholdMemberForm("Ada", 34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN,
                ActivityLevel.MODERATE, List.of(), null));

        verify(members).save(saved.capture());
        final HouseholdMember expectedMember = new HouseholdMember("alice", "Ada", 34, 180, new BigDecimal("82.5"),
                Sex.MALE, Goal.MAINTAIN, ActivityLevel.MODERATE, 2767, null, null);
        assertThat(saved.getValue()).usingRecursiveComparison().isEqualTo(expectedMember);
    }

    @Test
    void shouldStoreManualCaloriesAt400And6000() {
        final UserProfileRepository profiles = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final HouseholdMemberService service = service(profiles, members);
        final HouseholdMember current = stored(4L, "Ada");
        when(members.findByLoginOrderByMemberIdAsc("alice")).thenReturn(List.of(current));
        when(members.replaceConfirmedCalories(4L, "alice", 400)).thenReturn(1);
        when(members.replaceConfirmedCalories(4L, "alice", 6000)).thenReturn(1);

        final HouseholdMemberResult low = service.updateCalories("alice", 4L, 400);

        assertThat(low).usingRecursiveComparison().isEqualTo(HouseholdMemberResult.saved(new HouseholdMember(4L, "alice",
                "Ada", 34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN, ActivityLevel.MODERATE, 400, null,
                null)));

        final HouseholdMemberResult high = service.updateCalories("alice", 4L, 6000);

        verify(members).replaceConfirmedCalories(4L, "alice", 400);
        verify(members).replaceConfirmedCalories(4L, "alice", 6000);
        verify(profiles, never()).findById(any());
        assertThat(high).usingRecursiveComparison().isEqualTo(HouseholdMemberResult.saved(new HouseholdMember(4L,
                "alice", "Ada", 34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN, ActivityLevel.MODERATE, 6000,
                null, null)));
    }

    @Test
    void shouldRefuseManualCaloriesBelow400() {
        final UserProfileRepository profiles = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final HouseholdMemberService service = service(profiles, members);
        final HouseholdMember current = stored(4L, "Ada");

        final HouseholdMemberResult actual = service.updateCalories("alice", 4L, 399);

        verify(members, never()).replaceConfirmedCalories(anyLong(), any(), anyInt());
        verify(members, never()).findByLoginOrderByMemberIdAsc(any());
        verify(members, never()).findById(any());
        assertThat(current).usingRecursiveComparison().isEqualTo(stored(4L, "Ada"));
        assertThat(actual).usingRecursiveComparison()
                .isEqualTo(HouseholdMemberResult.rejected(HouseholdMemberError.CALORIES));
    }

    @Test
    void shouldLeaveThePreviousCaloriesWhenRecalculateIs166() {
        final UserProfileRepository profiles = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final HouseholdMemberService service = service(profiles, members);
        final HouseholdMember current = new HouseholdMember(4L, "alice", "Ada", 2, 70, new BigDecimal("8.0"),
                Sex.FEMALE, Goal.LOSE_WEIGHT, ActivityLevel.SEDENTARY, 795, null, null);
        when(members.findByLoginOrderByMemberIdAsc("alice")).thenReturn(List.of(current));

        final HouseholdMemberResult actual = service.recalculate("alice", 4L);

        verify(members, never()).replaceConfirmedCalories(anyLong(), any(), anyInt());
        verify(members, never()).findById(any());
        assertThat(current).usingRecursiveComparison().isEqualTo(new HouseholdMember(4L, "alice", "Ada", 2, 70,
                new BigDecimal("8.0"), Sex.FEMALE, Goal.LOSE_WEIGHT, ActivityLevel.SEDENTARY, 795, null, null));
        assertThat(actual).usingRecursiveComparison()
                .isEqualTo(HouseholdMemberResult.rejected(HouseholdMemberError.CALORIES));
    }

    @Test
    void shouldRevealNothingWhenUpdatingCaloriesOfAnotherAccountsId() {
        final UserProfileRepository profiles = mock(UserProfileRepository.class);
        final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
        final HouseholdMemberService service = service(profiles, members);
        when(members.findByLoginOrderByMemberIdAsc("alice")).thenReturn(List.of(stored(4L, "Ada")));

        final HouseholdMemberResult updated = service.updateCalories("alice", 9L, 500);
        final HouseholdMemberResult recalculated = service.recalculate("alice", 9L);

        verify(members, never()).replaceConfirmedCalories(anyLong(), any(), anyInt());
        verify(members, never()).findById(any());
        verify(profiles, never()).findById(any());
        assertThat(updated).usingRecursiveComparison()
                .isEqualTo(HouseholdMemberResult.rejected(HouseholdMemberError.NOT_FOUND));
        assertThat(recalculated).usingRecursiveComparison()
                .isEqualTo(HouseholdMemberResult.rejected(HouseholdMemberError.NOT_FOUND));
    }

    private HouseholdMemberService service(final UserProfileRepository profiles, final HouseholdMemberRepository members) {
        return new HouseholdMemberService(profiles, members, new ProductListFormat(), new CalorieService(),
                new DisplayNameValidator(), new PersonBoundsValidator(),
                new ProductListsValidator(new ProductNameValidator()));
    }

    private UserProfile owner(final String displayName) {
        return new UserProfile("alice", 34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN,
                ActivityLevel.MODERATE, 2767, null, null, null, displayName);
    }

    private HouseholdMember stored(final long memberId, final String displayName) {
        return new HouseholdMember(memberId, "alice", displayName, 34, 180, new BigDecimal("82.5"), Sex.MALE,
                Goal.MAINTAIN, ActivityLevel.MODERATE, 2767, null, null);
    }

    private HouseholdMemberForm form(final String displayName,
                                     final int age,
                                     final int heightCm,
                                     final BigDecimal weightKg,
                                     final Sex sex,
                                     final Goal goal,
                                     final ActivityLevel activityLevel) {
        return new HouseholdMemberForm(displayName, age, heightCm, weightKg, sex, goal, activityLevel, List.of(),
                List.of());
    }

    private void verifyOwnerUntouched(final UserProfileRepository profiles) {
        verify(profiles, never()).save(any());
        verify(profiles, never()).replaceConfirmedCalories(any(), anyInt());
        verify(profiles, never()).replaceBodyAndProducts(any(), anyInt(), anyInt(), any(), any(), any(), any(), any(),
                any(), any(), any());
        verify(profiles, never()).replaceBodyProductsAndCalories(any(), anyInt(), anyInt(), any(), any(), any(),
                any(), anyInt(), any(), any(), any(), any());
    }
}
