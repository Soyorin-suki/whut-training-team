package com.whut.training.service.impl;

import com.whut.training.common.TimeProvider;
import com.whut.training.repository.DailyProblemRepository;
import com.whut.training.repository.UserRepository;
import com.whut.training.service.CodeforcesApiService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailyProblemServiceImplTest {

    @Test
    void emptyProblemPoolStartsOnlyOneBackgroundSyncWithoutBlockingHome() throws Exception {
        DailyProblemRepository repository = mock(DailyProblemRepository.class);
        CodeforcesApiService codeforcesApiService = mock(CodeforcesApiService.class);
        UserRepository userRepository = mock(UserRepository.class);
        TimeProvider timeProvider = mock(TimeProvider.class);
        LocalDate today = LocalDate.of(2026, 8, 1);
        CountDownLatch fetchStarted = new CountDownLatch(1);
        CountDownLatch releaseFetch = new CountDownLatch(1);

        when(timeProvider.today()).thenReturn(today);
        when(repository.findDailySlotsByDate(today)).thenReturn(List.of());
        when(repository.countProblems()).thenReturn(0L);
        when(codeforcesApiService.fetchProblemSet()).thenAnswer(invocation -> {
            fetchStarted.countDown();
            releaseFetch.await(2, TimeUnit.SECONDS);
            return List.of();
        });

        DailyProblemServiceImpl service = new DailyProblemServiceImpl(
                repository,
                codeforcesApiService,
                userRepository,
                timeProvider,
                1200,
                2000,
                90,
                1700,
                true
        );

        try {
            assertTimeout(Duration.ofMillis(500), () -> assertFalse(service.ensureTodaySlots()));
            assertTrue(fetchStarted.await(1, TimeUnit.SECONDS));
            assertTimeout(Duration.ofMillis(500), () -> assertFalse(service.ensureTodaySlots()));
            verify(codeforcesApiService, times(1)).fetchProblemSet();
        } finally {
            releaseFetch.countDown();
        }
    }
}
