package com.whut.training.service;

import com.whut.training.domain.dto.FavoriteProblemItem;
import com.whut.training.domain.dto.ProblemFavoriteSummary;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.exception.BusinessException;
import com.whut.training.repository.DailyProblemRepository;
import com.whut.training.repository.ProblemFavoriteRepository;
import com.whut.training.service.impl.ProblemFavoriteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProblemFavoriteServiceImplTest {

    @Mock
    private ProblemFavoriteRepository problemFavoriteRepository;

    @Mock
    private DailyProblemRepository dailyProblemRepository;

    private ProblemFavoriteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProblemFavoriteServiceImpl(problemFavoriteRepository, dailyProblemRepository);
    }

    @Test
    void favoriteProblemRejectsUnknownProblemKey() {
        User user = new User(1L, "alice", "alice@example.com", "password123", UserRole.USER);

        when(dailyProblemRepository.existsProblemKey("2999-Z")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.favoriteProblem(user, "2999-Z"));

        assertEquals(404, ex.getCode());
        verify(problemFavoriteRepository, never()).insertFavorite(user.getId(), "2999-Z");
    }

    @Test
    void repeatedFavoriteKeepsIdempotentSummary() {
        User user = new User(2L, "alice", "alice@example.com", "password123", UserRole.USER);
        ProblemFavoriteSummary summary = new ProblemFavoriteSummary("2000-A", true, "2026-05-17T12:34:56+08:00");

        when(dailyProblemRepository.existsProblemKey("2000-A")).thenReturn(true);
        when(problemFavoriteRepository.findFavoriteStats(List.of("2000-A"), user.getId()))
                .thenReturn(Map.of("2000-A", summary));

        ProblemFavoriteSummary response = service.favoriteProblem(user, "2000-A");

        assertTrue(response.favoritedByMe());
        assertEquals("2026-05-17T12:34:56+08:00", response.favoritedAt());
        verify(problemFavoriteRepository).insertFavorite(user.getId(), "2000-A");
    }

    @Test
    void unfavoriteWithoutExistingRecordIsIdempotent() {
        User user = new User(3L, "alice", "alice@example.com", "password123", UserRole.USER);
        ProblemFavoriteSummary summary = new ProblemFavoriteSummary("2000-A", false, null);

        when(dailyProblemRepository.existsProblemKey("2000-A")).thenReturn(true);
        when(problemFavoriteRepository.findFavoriteStats(List.of("2000-A"), user.getId()))
                .thenReturn(Map.of("2000-A", summary));

        ProblemFavoriteSummary response = service.unfavoriteProblem(user, "2000-A");

        assertFalse(response.favoritedByMe());
        assertNull(response.favoritedAt());
        verify(problemFavoriteRepository).deleteFavorite(user.getId(), "2000-A");
    }

    @Test
    void getMyFavoritesReturnsPagedResponse() {
        User user = new User(4L, "alice", "alice@example.com", "password123", UserRole.USER);
        FavoriteProblemItem item = new FavoriteProblemItem(
                "2000-A",
                2000,
                "A",
                "Problem",
                1500,
                "dp",
                "https://codeforces.com/problemset/problem/2000/A",
                "DAILY",
                "2026-05-17T12:34:56+08:00"
        );

        when(problemFavoriteRepository.findUserFavorites(user.getId(), 1, 100)).thenReturn(List.of(item));
        when(problemFavoriteRepository.countUserFavorites(user.getId())).thenReturn(1L);

        var response = service.getMyFavorites(user, 0, 999);

        assertEquals(1, response.page());
        assertEquals(100, response.limit());
        assertEquals(1L, response.total());
        assertEquals(1, response.items().size());
        assertEquals("2000-A", response.items().get(0).problemKey());
    }
}
