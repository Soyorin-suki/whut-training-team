package com.whut.training.service;

import com.whut.training.domain.entity.CfProblem;
import com.whut.training.domain.entity.DailyProblem;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.entity.UserDailyStatus;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.exception.BusinessException;
import com.whut.training.repository.DailyProblemRepository;
import com.whut.training.repository.UserRepository;
import com.whut.training.service.impl.DailyProblemServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyProblemServiceImplTest {

    @Mock
    private DailyProblemRepository dailyProblemRepository;

    @Mock
    private CodeforcesApiService codeforcesApiService;

    @Mock
    private UserRepository userRepository;

    private DailyProblemServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DailyProblemServiceImpl(
                dailyProblemRepository,
                codeforcesApiService,
                1200,
                1600,
                90,
                userRepository
        );
    }

    @Test
    void checkInStartsAndExtendsStreaks() {
        User user = persistedUser(1L, 0, 0, 0);
        LocalDate today = LocalDate.now();
        DailyProblem dailyProblem = dailyProblem(today, 1500);

        when(dailyProblemRepository.findDailyByDate(today)).thenReturn(Optional.of(dailyProblem));
        when(dailyProblemRepository.findUserDailyStatus(user.getId(), today)).thenReturn(Optional.empty());
        when(dailyProblemRepository.findLatestUserDailyStatusDateBefore(user.getId(), today)).thenReturn(Optional.empty());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(codeforcesApiService.getSubmissionStatus(user.getUsername(), 1001L)).thenReturn(
                Optional.of(new CodeforcesApiService.SubmissionStatus(1001L, 2000, "A", "OK", Instant.now()))
        );

        service.checkIn(user, 1001L);

        verify(dailyProblemRepository).saveUserDailyStatus(user.getId(), today, 1001L, "OK", 1);
        verify(userRepository).updateUserScoreAndStreakStats(user.getId(), 1500, 1, 1);
    }

    @Test
    void checkInExtendsExistingStreakWhenYesterdayWasLastCheckIn() {
        User user = persistedUser(2L, 200, 1, 1);
        LocalDate today = LocalDate.now();
        DailyProblem dailyProblem = dailyProblem(today, 1200);

        when(dailyProblemRepository.findDailyByDate(today)).thenReturn(Optional.of(dailyProblem));
        when(dailyProblemRepository.findUserDailyStatus(user.getId(), today)).thenReturn(Optional.empty());
        when(dailyProblemRepository.findLatestUserDailyStatusDateBefore(user.getId(), today)).thenReturn(Optional.of(today.minusDays(1)));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(codeforcesApiService.getSubmissionStatus(user.getUsername(), 2001L)).thenReturn(
                Optional.of(new CodeforcesApiService.SubmissionStatus(2001L, 2000, "A", "OK", Instant.now()))
        );

        service.checkIn(user, 2001L);

        verify(userRepository).updateUserScoreAndStreakStats(user.getId(), 1400, 2, 2);
    }

    @Test
    void checkInResetsCurrentStreakAfterBreakButKeepsLongest() {
        User user = persistedUser(3L, 800, 3, 5);
        LocalDate today = LocalDate.now();
        DailyProblem dailyProblem = dailyProblem(today, 1000);

        when(dailyProblemRepository.findDailyByDate(today)).thenReturn(Optional.of(dailyProblem));
        when(dailyProblemRepository.findUserDailyStatus(user.getId(), today)).thenReturn(Optional.empty());
        when(dailyProblemRepository.findLatestUserDailyStatusDateBefore(user.getId(), today)).thenReturn(Optional.of(today.minusDays(2)));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(codeforcesApiService.getSubmissionStatus(user.getUsername(), 3001L)).thenReturn(
                Optional.of(new CodeforcesApiService.SubmissionStatus(3001L, 2000, "A", "OK", Instant.now()))
        );

        service.checkIn(user, 3001L);

        verify(userRepository).updateUserScoreAndStreakStats(user.getId(), 1800, 1, 5);
    }

    @Test
    void duplicateCheckInIsRejected() {
        User user = persistedUser(4L, 0, 0, 0);
        LocalDate today = LocalDate.now();

        when(dailyProblemRepository.findDailyByDate(today)).thenReturn(Optional.of(dailyProblem(today, 1000)));
        when(dailyProblemRepository.findUserDailyStatus(user.getId(), today)).thenReturn(
                Optional.of(new UserDailyStatus(user.getId(), today, 4001L, "OK", 1))
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> service.checkIn(user, 4001L));

        assertEquals(409, ex.getCode());
        verify(dailyProblemRepository, never()).saveUserDailyStatus(anyLong(), any(), anyLong(), any(), anyInt());
        verify(userRepository, never()).updateUserScoreAndStreakStats(anyLong(), any(), any(), any());
    }

    private User persistedUser(Long id, Integer score, Integer currentStreakDays, Integer longestStreakDays) {
        User user = new User(id, "alice", "alice@example.com", "password123", UserRole.USER);
        user.setScore(score);
        user.setCurrentStreakDays(currentStreakDays);
        user.setLongestStreakDays(longestStreakDays);
        return user;
    }

    private DailyProblem dailyProblem(LocalDate date, Integer rating) {
        return new DailyProblem(
                1L,
                date,
                "2000-A",
                2000,
                "A",
                "Problem",
                rating,
                "dp",
                "https://codeforces.com/problemset/problem/2000/A"
        );
    }
}
