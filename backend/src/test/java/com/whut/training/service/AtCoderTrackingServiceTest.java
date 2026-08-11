package com.whut.training.service;

import com.whut.training.common.TimeProvider;
import com.whut.training.domain.dto.AtCoderAbcDashboard;
import com.whut.training.domain.dto.UpcomingContestItem;
import com.whut.training.repository.AtCoderTrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AtCoderTrackingServiceTest {
    private final ContestService contestService = mock(ContestService.class);
    private final AtCoderApiService apiService = mock(AtCoderApiService.class);
    private final AtCoderTrackingRepository repository = mock(AtCoderTrackingRepository.class);
    private final TimeProvider timeProvider = mock(TimeProvider.class);
    private final AtCoderTrackingService service = new AtCoderTrackingService(
            contestService, apiService, repository, timeProvider
    );

    private final AtCoderTrackingRepository.ContestRow contest =
            new AtCoderTrackingRepository.ContestRow(
                    "abc460", "AtCoder Beginner Contest 460", 10_000L, 17_200L,
                    "https://atcoder.jp/contests/abc460", true, "DISCOVERED", null
            );
    private final AtCoderTrackingRepository.RequirementRow member =
            new AtCoderTrackingRepository.RequirementRow(
                    "abc460", 7L, "owl", "猫头鹰", null, "owl_atcoder",
                    false, null, false, null, null, null, null, null,
                    null, null, null, null, null
            );

    @BeforeEach
    void setUp() {
        when(timeProvider.nowEpochSecond()).thenReturn(20_000L);
        when(contestService.getAtCoderContestWindow()).thenReturn(List.of(new UpcomingContestItem(
                "ATCODER", "abc460", "AtCoder Beginner Contest 460", "Algorithm",
                Instant.ofEpochSecond(10_000L).toString(), 120, "~1999",
                "https://atcoder.jp/contests/abc460"
        )));
        when(repository.findContest("abc460")).thenReturn(java.util.Optional.of(contest));
        when(repository.findSyncCandidates(anyLong())).thenReturn(List.of(contest));
        when(repository.findRequirements("abc460")).thenReturn(List.of(member));
        when(repository.getSetting()).thenReturn(new AtCoderAbcDashboard.TrackingSetting(1, 24));
    }

    @Test
    void marksMemberCompletedAfterOfficialHistoryAndContestTimeAcAreConfirmed() {
        when(apiService.getHistory("owl_atcoder")).thenReturn(List.of(
                new AtCoderApiService.AtCoderHistoryEntry(
                        "abc460", "AtCoder Beginner Contest 460", true,
                        321, 1800, 1700, 1750, 17_200L
                )
        ));
        when(apiService.getAcceptedProblems("owl_atcoder", "abc460", 10_000L, 17_200L))
                .thenReturn(new AtCoderApiService.AcceptedProblems(1, List.of("abc460_a")));

        service.synchronize(true);

        verify(repository).upsertParticipation(
                "abc460", 7L, true, 321, 1800, true, 1700, 1750,
                1, "abc460_a", "COMPLETED", null
        );
        verify(repository).markContestSynced("abc460", "SYNCED");
    }

    @Test
    void recordsSourceErrorInsteadOfTreatingExternalApiFailureAsAbsence() {
        when(apiService.getHistory("owl_atcoder"))
                .thenThrow(new IllegalStateException("AtCoder history is unavailable"));

        service.synchronize(true);

        verify(repository).upsertParticipation(
                eq("abc460"), eq(7L), eq(false), eq(null), eq(null), eq(null), eq(null), eq(null),
                eq(null), eq(null), eq("DATA_ERROR"), any(String.class)
        );
        verify(repository).markContestSynced("abc460", "SYNCED");
    }
}
