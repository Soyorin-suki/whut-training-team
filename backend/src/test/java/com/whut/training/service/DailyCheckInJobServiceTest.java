package com.whut.training.service;

import com.whut.training.common.TimeProvider;
import com.whut.training.domain.dto.CheckInResultResponse;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailyCheckInJobServiceTest {

    @Test
    void returnsImmediatelyAndDeduplicatesTheSameSubmission() {
        DailyProblemService dailyProblemService = mock(DailyProblemService.class);
        TimeProvider timeProvider = mock(TimeProvider.class);
        LocalDate submittedDate = LocalDate.of(2026, 8, 2);
        when(timeProvider.today()).thenReturn(submittedDate);
        List<Runnable> queued = new ArrayList<>();
        Executor executor = queued::add;
        DailyCheckInJobService service = new DailyCheckInJobService(dailyProblemService, timeProvider, executor);
        User user = new User(7L, "owl", null, "password", UserRole.USER);
        when(dailyProblemService.checkIn(user, 123L, submittedDate))
                .thenReturn(new CheckInResultResponse("HARD", true, 123L, "OK", 1800));

        var pending = service.submit(user, 123L);
        var duplicate = service.submit(user, 123L);

        assertThat(pending.status()).isEqualTo("PENDING");
        assertThat(duplicate.jobId()).isEqualTo(pending.jobId());
        assertThat(queued).hasSize(1);
        queued.get(0).run();
        var finished = service.get(pending.jobId(), 7L);
        assertThat(finished.status()).isEqualTo("SUCCEEDED");
        assertThat(finished.result().score()).isEqualTo(1800);
        verify(dailyProblemService, times(1)).checkIn(user, 123L, submittedDate);
        assertThatThrownBy(() -> service.get(pending.jobId(), 8L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("forbidden");
    }
}
