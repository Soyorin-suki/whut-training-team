package com.whut.training.config;

import com.whut.training.repository.DailyProblemRepository;
import com.whut.training.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserStreakBackfillInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DailyProblemRepository dailyProblemRepository;

    @Test
    void backfillsCurrentAndLongestStreaksFromHistory() {
        LocalDate today = LocalDate.now();
        when(userRepository.findUserIdsRequiringStreakBackfill()).thenReturn(List.of(1L, 2L));
        when(dailyProblemRepository.findAllUserDailyCheckInDates()).thenReturn(Map.of(
                1L, List.of(
                        today.minusDays(2),
                        today.minusDays(1),
                        today
                ),
                2L, List.of(
                        today.minusDays(10),
                        today.minusDays(8),
                        today.minusDays(7)
                )
        ));

        new UserStreakBackfillInitializer(userRepository, dailyProblemRepository).backfill();

        verify(userRepository).updateUserStreakStats(1L, 3, 3);
        verify(userRepository).updateUserStreakStats(2L, 0, 2);
    }

    @Test
    void skipsBackfillWhenNoCandidatesExist() {
        when(userRepository.findUserIdsRequiringStreakBackfill()).thenReturn(List.of());

        new UserStreakBackfillInitializer(userRepository, dailyProblemRepository).backfill();

        verify(dailyProblemRepository, never()).findAllUserDailyCheckInDates();
        verify(userRepository, never()).updateUserStreakStats(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
