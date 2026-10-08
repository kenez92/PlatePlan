package com.kenez92.plateplan.profile.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.kenez92.plateplan.profile.db.UserProfile;
import com.kenez92.plateplan.profile.db.UserProfileRepository;
import com.kenez92.plateplan.profile.model.ActivityLevel;
import com.kenez92.plateplan.profile.model.ConfirmedCaloriesResult;
import com.kenez92.plateplan.profile.model.Goal;
import com.kenez92.plateplan.profile.model.Sex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmedCaloriesServiceTest {

    @Test
    void shouldReturnTheStoredDailyCalories() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final ConfirmedCaloriesService service = service(repository);
        when(repository.findById("alice")).thenReturn(Optional.of(storedRow(2000)));

        final ConfirmedCaloriesResult actual = service.load("alice");

        verify(repository, never()).save(any());
        final ConfirmedCaloriesResult expected = ConfirmedCaloriesResult.loaded(2000);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnEmptyCaloriesWhenThereIsNoRow() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final ConfirmedCaloriesService service = service(repository);
        when(repository.findById("alice")).thenReturn(Optional.empty());

        final ConfirmedCaloriesResult actual = service.load("alice");

        final ConfirmedCaloriesResult expected = ConfirmedCaloriesResult.loaded(null);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReplaceOnlyTheDailyCalories() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final ConfirmedCaloriesService service = service(repository);
        when(repository.replaceConfirmedCalories("alice", 2000)).thenReturn(1);

        final ConfirmedCaloriesResult actual = service.update("alice", 2000);

        verify(repository).replaceConfirmedCalories("alice", 2000);
        verify(repository, never()).save(any());
        verify(repository, never()).findById(any());
        verify(repository, never()).replaceBodyAndProducts(any(), anyInt(), anyInt(), any(), any(), any(), any(), any(),
                any());
        verify(repository, never()).replaceBodyProductsAndCalories(any(), anyInt(), anyInt(), any(), any(), any(),
                any(), anyInt(), any(), any());
        final ConfirmedCaloriesResult expected = ConfirmedCaloriesResult.saved(2000);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldRecalculateOnlyTheDailyCaloriesFromTheStoredBody() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final ConfirmedCaloriesService service = service(repository);
        when(repository.findById("alice")).thenReturn(Optional.of(storedRow(2000)));
        when(repository.replaceConfirmedCalories("alice", 2767)).thenReturn(1);

        final ConfirmedCaloriesResult actual = service.recalculate("alice");

        verify(repository).replaceConfirmedCalories("alice", 2767);
        verify(repository, never()).save(any());
        verify(repository, never()).replaceBodyAndProducts(any(), anyInt(), anyInt(), any(), any(), any(), any(), any(),
                any());
        verify(repository, never()).replaceBodyProductsAndCalories(any(), anyInt(), anyInt(), any(), any(), any(),
                any(), anyInt(), any(), any());
        final ConfirmedCaloriesResult expected = ConfirmedCaloriesResult.saved(2767);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnMissingWhenThereIsNoRowToUpdateCalories() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final ConfirmedCaloriesService service = service(repository);
        when(repository.replaceConfirmedCalories("alice", 2000)).thenReturn(0);

        final ConfirmedCaloriesResult actual = service.update("alice", 2000);

        verify(repository, never()).save(any());
        final ConfirmedCaloriesResult expected = ConfirmedCaloriesResult.noProfile();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnMissingWhenThereIsNoRowToRecalculate() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final ConfirmedCaloriesService service = service(repository);
        when(repository.findById("alice")).thenReturn(Optional.empty());

        final ConfirmedCaloriesResult actual = service.recalculate("alice");

        verify(repository, never()).replaceConfirmedCalories(any(), anyInt());
        verify(repository, never()).save(any());
        final ConfirmedCaloriesResult expected = ConfirmedCaloriesResult.noProfile();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnMissingWhenTheUpdateTouchesNoRow() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final ConfirmedCaloriesService service = service(repository);
        when(repository.findById("alice")).thenReturn(Optional.of(storedRow(2000)));
        when(repository.replaceConfirmedCalories("alice", 2767)).thenReturn(0);

        final ConfirmedCaloriesResult actual = service.recalculate("alice");

        verify(repository, never()).save(any());
        final ConfirmedCaloriesResult expected = ConfirmedCaloriesResult.noProfile();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReportUnavailableWhenTheReadFails() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final ConfirmedCaloriesService service = service(repository);
        when(repository.findById("alice"))
                .thenThrow(new DataAccessResourceFailureException("The database is unreachable"));

        final ConfirmedCaloriesResult actual = service.load("alice");

        final ConfirmedCaloriesResult expected = ConfirmedCaloriesResult.failed();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReportUnavailableWhenTheUpdateFails() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final ConfirmedCaloriesService service = service(repository);
        when(repository.replaceConfirmedCalories("alice", 2000))
                .thenThrow(new DataAccessResourceFailureException("The database is unreachable"));

        final ConfirmedCaloriesResult actual = service.update("alice", 2000);

        verify(repository, never()).save(any());
        final ConfirmedCaloriesResult expected = ConfirmedCaloriesResult.failed();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldLogOnlyTheClassNamesWhenTheDatabaseFails() {
        final UserProfileRepository repository = mock(UserProfileRepository.class);
        final ConfirmedCaloriesService service = service(repository);
        when(repository.replaceConfirmedCalories("alice", 2000))
                .thenThrow(new DataAccessResourceFailureException("alice, 2000, jajka"));
        final Logger logger = (Logger) LoggerFactory.getLogger(ConfirmedCaloriesService.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            service.update("alice", 2000);
        } finally {
            logger.detachAppender(appender);
        }

        final List<String> messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)).contains(DataAccessResourceFailureException.class.getName());
        assertThat(messages.get(0)).doesNotContain("alice", "2000", "jajka");
    }

    private ConfirmedCaloriesService service(final UserProfileRepository repository) {
        return new ConfirmedCaloriesService(repository, new CalorieService());
    }

    private UserProfile storedRow(final Integer confirmedCalories) {
        return new UserProfile("alice", 34, 180, new BigDecimal("82.5"), Sex.MALE, Goal.MAINTAIN,
                ActivityLevel.MODERATE, confirmedCalories, "jajka", "orzechy");
    }
}
