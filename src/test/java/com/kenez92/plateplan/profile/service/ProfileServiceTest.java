package com.kenez92.plateplan.profile.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.kenez92.plateplan.profile.controller.dto.ProfileFormDto;
import com.kenez92.plateplan.profile.db.UserProfile;
import com.kenez92.plateplan.profile.db.UserProfileRepository;
import com.kenez92.plateplan.profile.model.ActivityLevel;
import com.kenez92.plateplan.profile.model.Goal;
import com.kenez92.plateplan.profile.format.ProductListFormat;
import com.kenez92.plateplan.profile.model.ProfileResult;
import com.kenez92.plateplan.profile.model.Sex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Test
    void shouldReturnAnEmptyInputWhenTheAccountHasNoRow() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final ProfileService service = service(repository);
        when(repository.findById("alice")).thenReturn(Optional.empty());

        final ProfileResult actual = service.load("alice");

        final ProfileResult expected = ProfileResult.loaded(ProfileFormDto.empty());
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnTheStoredValuesInTheirFormFormat() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final ProfileService service = service(repository);
        when(repository.findById("alice")).thenReturn(Optional.of(new UserProfile("alice", 34, 180,
                new BigDecimal("82.0"), Sex.MALE, Goal.MAINTAIN, ActivityLevel.MODERATE,
                "mleko 3,2%;jajka;ser", "orzechy")));

        final ProfileResult actual = service.load("alice");

        final ProfileResult expected = ProfileResult.loaded(new ProfileFormDto(34, 180, new BigDecimal("82.0"),
                Sex.MALE, Goal.MAINTAIN, ActivityLevel.MODERATE, "mleko 3,2%;jajka;ser", "orzechy"));
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldCreateTheRowOnTheFirstSave() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final ProfileService service = service(repository);
        final ArgumentCaptor<UserProfile> saved = ArgumentCaptor.forClass(UserProfile.class);
        when(repository.findById("alice")).thenReturn(Optional.empty());
        when(repository.save(any(UserProfile.class))).then(returnsFirstArg());
        final ProfileFormDto form = new ProfileFormDto(34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN,
                ActivityLevel.MODERATE, "mleko 3,2%;jajka", "orzechy");

        final ProfileResult actual = service.save("alice", form);

        verify(repository).save(saved.capture());
        final UserProfile expectedRow = new UserProfile("alice", 34, 180, new BigDecimal("82.5"), Sex.MALE,
                Goal.MAINTAIN, ActivityLevel.MODERATE, "mleko 3,2%;jajka", "orzechy");
        final ProfileResult expected = ProfileResult.saved(new ProfileFormDto(34, 180, new BigDecimal("82.5"),
                Sex.MALE, Goal.MAINTAIN, ActivityLevel.MODERATE, "mleko 3,2%;jajka", "orzechy"));
        assertThat(saved.getValue()).usingRecursiveComparison().isEqualTo(expectedRow);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReplaceEveryValueOfTheExistingRowOnSave() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final ProfileService service = service(repository);
        final ArgumentCaptor<UserProfile> saved = ArgumentCaptor.forClass(UserProfile.class);
        when(repository.findById("alice")).thenReturn(Optional.of(new UserProfile("alice", 50, 160,
                new BigDecimal("60.0"), Sex.FEMALE, Goal.GAIN, ActivityLevel.HIGH, "ryba", "mleko")));
        when(repository.save(any(UserProfile.class))).then(returnsFirstArg());
        final ProfileFormDto form = new ProfileFormDto(34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN,
                ActivityLevel.MODERATE, "jajka", "");

        service.save("alice", form);

        verify(repository).save(saved.capture());
        final UserProfile expectedRow = new UserProfile("alice", 34, 180, new BigDecimal("82.5"), Sex.MALE,
                Goal.MAINTAIN, ActivityLevel.MODERATE, "jajka", null);
        assertThat(saved.getValue()).usingRecursiveComparison().isEqualTo(expectedRow);
    }

    @Test
    void shouldStoreNullWhenBothProductListsAreEmpty() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final ProfileService service = service(repository);
        final ArgumentCaptor<UserProfile> saved = ArgumentCaptor.forClass(UserProfile.class);
        when(repository.findById("alice")).thenReturn(Optional.empty());
        when(repository.save(any(UserProfile.class))).then(returnsFirstArg());
        final ProfileFormDto form = new ProfileFormDto(34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN,
                ActivityLevel.MODERATE, "", null);

        service.save("alice", form);

        verify(repository).save(saved.capture());
        final UserProfile expectedRow = new UserProfile("alice", 34, 180, new BigDecimal("82.5"), Sex.MALE,
                Goal.MAINTAIN, ActivityLevel.MODERATE, null, null);
        assertThat(saved.getValue()).usingRecursiveComparison().isEqualTo(expectedRow);
    }

    @Test
    void shouldLookTheRowUpByTheExactLogin() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final ProfileService service = service(repository);
        final ArgumentCaptor<UserProfile> saved = ArgumentCaptor.forClass(UserProfile.class);
        when(repository.findById("Alice")).thenReturn(Optional.of(new UserProfile("Alice", 50, 160,
                new BigDecimal("60.0"), Sex.FEMALE, Goal.GAIN, ActivityLevel.HIGH, "ryba", "mleko")));
        when(repository.save(any(UserProfile.class))).then(returnsFirstArg());
        final ProfileFormDto form = new ProfileFormDto(34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN,
                ActivityLevel.MODERATE, "jajka", "");

        service.save("Alice", form);

        verify(repository).save(saved.capture());
        final UserProfile expectedRow = new UserProfile("Alice", 34, 180, new BigDecimal("82.5"), Sex.MALE,
                Goal.MAINTAIN, ActivityLevel.MODERATE, "jajka", null);
        assertThat(saved.getValue()).usingRecursiveComparison().isEqualTo(expectedRow);
    }

    @Test
    void shouldKeepTheTypedCaseOfProducts() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final ProfileService service = service(repository);
        when(repository.findById("alice")).thenReturn(Optional.empty());
        when(repository.save(any(UserProfile.class))).then(returnsFirstArg());
        final ProfileFormDto form = new ProfileFormDto(34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN,
                ActivityLevel.MODERATE, "Mleko;JAJKA", "Orzechy");

        final ProfileResult actual = service.save("alice", form);

        final ProfileResult expected = ProfileResult.saved(new ProfileFormDto(34, 180, new BigDecimal("82.5"),
                Sex.MALE, Goal.MAINTAIN, ActivityLevel.MODERATE, "Mleko;JAJKA", "Orzechy"));
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReportUnavailableWhenTheReadFails() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final ProfileService service = service(repository);
        when(repository.findById("alice"))
                .thenThrow(new DataAccessResourceFailureException("The database is unreachable"));

        final ProfileResult actual = service.load("alice");

        final ProfileResult expected = ProfileResult.unavailable(ProfileFormDto.empty());
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReportUnavailableWhenTheSaveFails() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final ProfileService service = service(repository);
        when(repository.findById("alice")).thenReturn(Optional.empty());
        when(repository.save(any(UserProfile.class)))
                .thenThrow(new DataAccessResourceFailureException("The database is unreachable"));
        final ProfileFormDto form = new ProfileFormDto(34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN,
                ActivityLevel.MODERATE, "jajka", "orzechy");

        final ProfileResult actual = service.save("alice", form);

        final ProfileResult expected = ProfileResult.unavailable(form);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReportUnavailableWhenTheLookupBeforeTheSaveFails() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final ProfileService service = service(repository);
        when(repository.findById("alice"))
                .thenThrow(new DataAccessResourceFailureException("The database is unreachable"));
        final ProfileFormDto form = new ProfileFormDto(34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN,
                ActivityLevel.MODERATE, "jajka", "orzechy");

        final ProfileResult actual = service.save("alice", form);

        final ProfileResult expected = ProfileResult.unavailable(form);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReportUnavailableOnAUniqueIndexViolation() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final ProfileService service = service(repository);
        when(repository.findById("alice")).thenReturn(Optional.empty());
        when(repository.save(any(UserProfile.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicate key violates user_profile_pkey, Key (login)=(alice) already exists"));
        final ProfileFormDto form = new ProfileFormDto(34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN,
                ActivityLevel.MODERATE, "jajka", "orzechy");

        final ProfileResult actual = service.save("alice", form);

        final ProfileResult expected = ProfileResult.unavailable(form);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldLogOnlyTheClassNamesWhenTheDatabaseFails() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final ProfileService service = service(repository);
        when(repository.findById("alice")).thenReturn(Optional.empty());
        when(repository.save(any(UserProfile.class)))
                .thenThrow(new DataIntegrityViolationException("Key (login)=(alice) is unreachable, kefir, 82.5"));
        final ProfileFormDto form = new ProfileFormDto(34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN,
                ActivityLevel.MODERATE, "kefir", "orzechy");
        final Logger logger = (Logger) LoggerFactory.getLogger(ProfileService.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            service.save("alice", form);
        } finally {
            logger.detachAppender(appender);
        }

        final List<String> messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)).contains(DataIntegrityViolationException.class.getName());
        assertThat(messages.get(0)).doesNotContain("alice", "unreachable", "kefir", "orzechy", "82.5", "MALE");
    }

    private ProfileService service(final UserProfileRepository repository) {
        return new ProfileService(repository, new ProductListFormat());
    }
}
