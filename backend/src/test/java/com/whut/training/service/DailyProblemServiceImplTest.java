package com.whut.training.service;

import com.whut.training.domain.dto.PracticeHistoryItem;
import com.whut.training.domain.entity.CfProblem;
import com.whut.training.domain.entity.DailyProblem;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.entity.UserDailyStatus;
import com.whut.training.domain.entity.UserPracticeDraw;
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
import java.util.Map;
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

    @Mock
    private DailyProblemCacheService dailyProblemCacheService;

    @Mock
    private ProblemLikeService problemLikeService;

    @Mock
    private ProblemFavoriteService problemFavoriteService;

    private DailyProblemServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DailyProblemServiceImpl(
                dailyProblemRepository,
                codeforcesApiService,
                1200,
                1600,
                90,
                userRepository,
                dailyProblemCacheService,
                problemLikeService,
                problemFavoriteService
        );
    }

    @Test
    void getTodayUsesCachedDailyProblemWhenAvailable() {
        User user = persistedUser(10L, 0, 0, 0);
        LocalDate today = LocalDate.now();
        DailyProblem cachedProblem = dailyProblem(today, 1500);

        when(dailyProblemCacheService.get(today)).thenReturn(Optional.of(cachedProblem));
        when(dailyProblemRepository.findUserDailyStatus(user.getId(), today)).thenReturn(Optional.empty());
        when(problemLikeService.getLikeStats(List.of("2000-A"), user.getId())).thenReturn(Map.of());
        when(problemFavoriteService.getFavoriteStats(List.of("2000-A"), user.getId())).thenReturn(Map.of());

        var response = service.getToday(user);

        assertEquals("2000-A", response.problem().problemKey());
        assertEquals(1500, response.problem().rating());
        verify(dailyProblemRepository, never()).findDailyByDate(today);
    }

    @Test
    void getTodayBackfillsCacheAfterDatabaseHit() {
        User user = persistedUser(11L, 0, 0, 0);
        LocalDate today = LocalDate.now();
        DailyProblem storedProblem = dailyProblem(today, 1600);

        when(dailyProblemCacheService.get(today)).thenReturn(Optional.empty());
        when(dailyProblemRepository.findDailyByDate(today)).thenReturn(Optional.of(storedProblem));
        when(dailyProblemRepository.findUserDailyStatus(user.getId(), today)).thenReturn(Optional.empty());
        when(problemLikeService.getLikeStats(List.of("2000-A"), user.getId())).thenReturn(Map.of());
        when(problemFavoriteService.getFavoriteStats(List.of("2000-A"), user.getId())).thenReturn(Map.of());

        var response = service.getToday(user);

        assertEquals("2000-A", response.problem().problemKey());
        verify(dailyProblemCacheService).put(storedProblem);
    }

    @Test
    void getTodayIncludesProblemLikeSummary() {
        User user = persistedUser(12L, 0, 0, 0);
        LocalDate today = LocalDate.now();
        DailyProblem storedProblem = dailyProblem(today, 1700);

        when(dailyProblemCacheService.get(today)).thenReturn(Optional.of(storedProblem));
        when(dailyProblemRepository.findUserDailyStatus(user.getId(), today)).thenReturn(Optional.empty());
        when(problemLikeService.getLikeStats(List.of("2000-A"), user.getId())).thenReturn(Map.of(
                "2000-A",
                new com.whut.training.domain.dto.ProblemLikeSummary("2000-A", 3, true)
        ));
        when(problemFavoriteService.getFavoriteStats(List.of("2000-A"), user.getId())).thenReturn(Map.of());

        var response = service.getToday(user);

        assertEquals(3, response.problem().likeCount());
        assertEquals(true, response.problem().likedByMe());
    }

    @Test
    void getTodayIncludesProblemFavoriteSummary() {
        User user = persistedUser(13L, 0, 0, 0);
        LocalDate today = LocalDate.now();
        DailyProblem storedProblem = dailyProblem(today, 1700);

        when(dailyProblemCacheService.get(today)).thenReturn(Optional.of(storedProblem));
        when(dailyProblemRepository.findUserDailyStatus(user.getId(), today)).thenReturn(Optional.empty());
        when(problemLikeService.getLikeStats(List.of("2000-A"), user.getId())).thenReturn(Map.of());
        when(problemFavoriteService.getFavoriteStats(List.of("2000-A"), user.getId())).thenReturn(Map.of(
                "2000-A",
                new com.whut.training.domain.dto.ProblemFavoriteSummary(
                        "2000-A",
                        true,
                        "2026-05-17T12:34:56+08:00"
                )
        ));

        var response = service.getToday(user);

        assertEquals(true, response.problem().favoritedByMe());
        assertEquals("2026-05-17T12:34:56+08:00", response.problem().favoritedAt());
    }

    @Test
    void getHistoryIncludesFavoriteSummary() {
        User user = persistedUser(14L, 0, 0, 0);
        LocalDate today = LocalDate.now();
        DailyProblem storedProblem = dailyProblem(today, 1500);

        when(dailyProblemCacheService.get(today)).thenReturn(Optional.of(storedProblem));
        when(dailyProblemRepository.findDailyProblemsByDateRange(today, today)).thenReturn(List.of(storedProblem));
        when(dailyProblemRepository.findUserDailyStatusByDateRange(user.getId(), today, today)).thenReturn(Map.of());
        when(problemLikeService.getLikeStats(List.of("2000-A"), user.getId())).thenReturn(Map.of());
        when(problemFavoriteService.getFavoriteStats(List.of("2000-A"), user.getId())).thenReturn(Map.of(
                "2000-A",
                new com.whut.training.domain.dto.ProblemFavoriteSummary("2000-A", true, "2026-05-17T08:00:00Z")
        ));

        var historyItems = service.getHistory(user, 1);

        assertEquals(1, historyItems.size());
        assertEquals(true, historyItems.get(0).favoritedByMe());
        assertEquals("2026-05-17T08:00:00Z", historyItems.get(0).favoritedAt());
    }

    @Test
    void getPracticeHistoryIncludesFavoriteSummary() {
        User user = persistedUser(15L, 0, 0, 0);
        PracticeHistoryItem item = new PracticeHistoryItem(
                10L,
                LocalDate.now().toString(),
                "2000-A",
                "Problem",
                1600,
                "https://codeforces.com/problemset/problem/2000/A",
                9001L,
                "OK",
                "2026-05-17T08:00:00Z",
                0,
                false,
                false,
                null
        );

        when(dailyProblemRepository.findCheckedPracticeHistory(user.getId(), 5)).thenReturn(List.of(item));
        when(problemLikeService.getLikeStats(List.of("2000-A"), user.getId())).thenReturn(Map.of());
        when(problemFavoriteService.getFavoriteStats(List.of("2000-A"), user.getId())).thenReturn(Map.of(
                "2000-A",
                new com.whut.training.domain.dto.ProblemFavoriteSummary("2000-A", true, "2026-05-17T08:00:00Z")
        ));

        var items = service.getPracticeHistory(user, 5);

        assertEquals(1, items.size());
        assertEquals(true, items.get(0).favoritedByMe());
        assertEquals("2026-05-17T08:00:00Z", items.get(0).favoritedAt());
    }

    @Test
    void drawPracticeProblemIncludesFavoriteSummary() {
        User user = persistedUser(16L, 0, 0, 0);
        CfProblem selectedProblem = new CfProblem(
                "1888-C",
                1888,
                "C",
                "Practice Problem",
                1700,
                "graphs",
                false,
                null,
                100,
                "https://codeforces.com/problemset/problem/1888/C"
        );
        UserPracticeDraw draw = new UserPracticeDraw(
                21L,
                user.getId(),
                LocalDate.now(),
                "1888-C",
                1888,
                "C",
                "Practice Problem",
                1700,
                "graphs",
                "https://codeforces.com/problemset/problem/1888/C",
                null,
                null
        );

        when(dailyProblemRepository.countProblems()).thenReturn(1L);
        when(dailyProblemRepository.findRandomProblem(1200, 1600, List.of())).thenReturn(Optional.of(selectedProblem));
        when(dailyProblemRepository.insertPracticeDraw(user.getId(), LocalDate.now(), selectedProblem)).thenReturn(draw);
        when(problemLikeService.getLikeStats(List.of("1888-C"), user.getId())).thenReturn(Map.of());
        when(problemFavoriteService.getFavoriteStats(List.of("1888-C"), user.getId())).thenReturn(Map.of(
                "1888-C",
                new com.whut.training.domain.dto.ProblemFavoriteSummary("1888-C", true, "2026-05-17T09:00:00Z")
        ));

        var response = service.drawPracticeProblem(user, null, null, null);

        assertEquals(true, response.problem().favoritedByMe());
        assertEquals("2026-05-17T09:00:00Z", response.problem().favoritedAt());
    }

    @Test
    void checkInStartsAndExtendsStreaks() {
        User user = persistedUser(1L, 0, 0, 0);
        LocalDate today = LocalDate.now();
        DailyProblem dailyProblem = dailyProblem(today, 1500);

        when(dailyProblemCacheService.get(today)).thenReturn(Optional.of(dailyProblem));
        when(dailyProblemRepository.findUserDailyStatus(user.getId(), today)).thenReturn(Optional.empty());
        when(dailyProblemRepository.findLatestUserDailyStatusDateBefore(user.getId(), today)).thenReturn(Optional.empty());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(codeforcesApiService.getSubmissionStatus(user.getUsername(), 1001L)).thenReturn(
                Optional.of(new CodeforcesApiService.SubmissionStatus(1001L, 2000, "A", "OK", Instant.now()))
        );

        service.checkIn(user, 1001L);

        verify(dailyProblemRepository).saveUserDailyStatus(user.getId(), today, 1001L, "OK", 1);
        verify(userRepository).updateUserScoreAndStreakStats(user.getId(), 1, 1, 1);
    }

    @Test
    void checkInExtendsExistingStreakWhenYesterdayWasLastCheckIn() {
        User user = persistedUser(2L, 200, 1, 1);
        LocalDate today = LocalDate.now();
        DailyProblem dailyProblem = dailyProblem(today, 1200);

        when(dailyProblemCacheService.get(today)).thenReturn(Optional.of(dailyProblem));
        when(dailyProblemRepository.findUserDailyStatus(user.getId(), today)).thenReturn(Optional.empty());
        when(dailyProblemRepository.findLatestUserDailyStatusDateBefore(user.getId(), today)).thenReturn(Optional.of(today.minusDays(1)));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(codeforcesApiService.getSubmissionStatus(user.getUsername(), 2001L)).thenReturn(
                Optional.of(new CodeforcesApiService.SubmissionStatus(2001L, 2000, "A", "OK", Instant.now()))
        );

        service.checkIn(user, 2001L);

        verify(userRepository).updateUserScoreAndStreakStats(user.getId(), 201, 2, 2);
    }

    @Test
    void checkInResetsCurrentStreakAfterBreakButKeepsLongest() {
        User user = persistedUser(3L, 800, 3, 5);
        LocalDate today = LocalDate.now();
        DailyProblem dailyProblem = dailyProblem(today, 1000);

        when(dailyProblemCacheService.get(today)).thenReturn(Optional.of(dailyProblem));
        when(dailyProblemRepository.findUserDailyStatus(user.getId(), today)).thenReturn(Optional.empty());
        when(dailyProblemRepository.findLatestUserDailyStatusDateBefore(user.getId(), today)).thenReturn(Optional.of(today.minusDays(2)));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(codeforcesApiService.getSubmissionStatus(user.getUsername(), 3001L)).thenReturn(
                Optional.of(new CodeforcesApiService.SubmissionStatus(3001L, 2000, "A", "OK", Instant.now()))
        );

        service.checkIn(user, 3001L);

        verify(userRepository).updateUserScoreAndStreakStats(user.getId(), 801, 1, 5);
    }

    @Test
    void duplicateCheckInIsRejected() {
        User user = persistedUser(4L, 0, 0, 0);
        LocalDate today = LocalDate.now();

        when(dailyProblemCacheService.get(today)).thenReturn(Optional.of(dailyProblem(today, 1000)));
        when(dailyProblemRepository.findUserDailyStatus(user.getId(), today)).thenReturn(
                Optional.of(new UserDailyStatus(user.getId(), today, 4001L, "OK", 1))
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> service.checkIn(user, 4001L));

        assertEquals(409, ex.getCode());
        verify(dailyProblemRepository, never()).saveUserDailyStatus(anyLong(), any(), anyLong(), any(), anyInt());
        verify(userRepository, never()).updateUserScoreAndStreakStats(anyLong(), any(), any(), any());
    }

    @Test
    void regenerateTodayEvictsAndRefreshesCache() {
        User admin = new User(99L, "admin", "admin@example.com", "password123", UserRole.ADMIN);
        LocalDate today = LocalDate.now();
        CfProblem selectedProblem = new CfProblem(
                "1888-C",
                1888,
                "C",
                "New Daily",
                1700,
                "graphs",
                false,
                null,
                100,
                "https://codeforces.com/problemset/problem/1888/C"
        );
        DailyProblem generated = new DailyProblem(
                2L,
                today,
                selectedProblem.problemKey(),
                selectedProblem.contestId(),
                selectedProblem.problemIndex(),
                selectedProblem.name(),
                selectedProblem.rating(),
                selectedProblem.tags(),
                selectedProblem.sourceUrl()
        );

        when(dailyProblemRepository.countProblems()).thenReturn(1L);
        when(dailyProblemRepository.findRandomProblem(eq(1200), eq(1600), any(LocalDate.class)))
                .thenReturn(Optional.of(selectedProblem));
        when(dailyProblemRepository.insertDailyProblem(today, selectedProblem, "admin")).thenReturn(generated);
        when(problemLikeService.getLikeStats(List.of("1888-C"), admin.getId())).thenReturn(Map.of());
        when(problemFavoriteService.getFavoriteStats(List.of("1888-C"), admin.getId())).thenReturn(Map.of());

        var response = service.regenerateTodayByAdmin(admin);

        assertEquals("1888-C", response.problemKey());
        verify(dailyProblemCacheService).evict(today);
        verify(dailyProblemRepository).deleteDailyByDate(today);
        verify(dailyProblemCacheService).put(generated);
    }

    @Test
    void regenerateTodayIsRejectedAfterCheckInsExist() {
        User admin = new User(100L, "admin", "admin@example.com", "password123", UserRole.ADMIN);
        LocalDate today = LocalDate.now();

        when(dailyProblemRepository.countDailyCheckIns(today)).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.regenerateTodayByAdmin(admin));

        assertEquals(409, ex.getCode());
        verify(dailyProblemCacheService, never()).evict(today);
        verify(dailyProblemRepository, never()).deleteDailyByDate(today);
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
