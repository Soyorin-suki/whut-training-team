package com.whut.training.service;

import com.whut.training.common.TimeProvider;
import com.whut.training.domain.dto.FunCheckInItem;
import com.whut.training.repository.FunCheckInRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FunCheckInServiceTest {

    private final FunCheckInRepository repository = mock(FunCheckInRepository.class);
    private final TimeProvider timeProvider = mock(TimeProvider.class);
    private final FortuneCatalog fortuneCatalog = mock(FortuneCatalog.class);
    private final FunCheckInService service = new FunCheckInService(repository, timeProvider, fortuneCatalog);

    @Test
    void createsStableFortuneForFirstCheckIn() {
        LocalDate today = LocalDate.of(2026, 8, 9);
        when(timeProvider.today()).thenReturn(today);
        when(timeProvider.now()).thenReturn(LocalDateTime.of(2026, 8, 9, 13, 20));
        when(repository.findByUserAndDate(2L, today)).thenReturn(Optional.empty());
        when(fortuneCatalog.select(2L, today)).thenReturn(new FortuneCatalog.Fortune(
                "CLEAR", "大吉", "保持专注", "动态规划", "#2563eb", 5
        ));

        FunCheckInItem result = service.checkIn(2L);

        ArgumentCaptor<FunCheckInItem> captor = ArgumentCaptor.forClass(FunCheckInItem.class);
        verify(repository).insert(captor.capture());
        assertEquals("2026-08-09", result.date());
        assertNotNull(result.fortuneTitle());
        assertEquals(result.fortuneKey(), captor.getValue().fortuneKey());
    }

    @Test
    void returnsExistingFortuneWithoutDrawingAgain() {
        LocalDate today = LocalDate.of(2026, 8, 9);
        FunCheckInItem existing = new FunCheckInItem(
                9L, 2L, "2026-08-09", "CLEAR", "大吉", "保持专注",
                "动态规划", "#2563eb", 5, "2026-08-09T09:00:00"
        );
        when(timeProvider.today()).thenReturn(today);
        when(repository.findByUserAndDate(2L, today)).thenReturn(Optional.of(existing));

        assertEquals(existing, service.checkIn(2L));
        verify(repository, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void loadsRequestedHistoryWindow() {
        when(timeProvider.today()).thenReturn(LocalDate.of(2026, 8, 9));
        when(repository.findRange(2L, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 9)))
                .thenReturn(List.of());

        service.history(2L, 7);

        verify(repository).findRange(2L, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 9));
    }
}
