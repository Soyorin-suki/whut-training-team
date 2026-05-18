package com.whut.training.service;

import com.whut.training.domain.dto.ProblemLikeSummary;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.exception.BusinessException;
import com.whut.training.repository.DailyProblemRepository;
import com.whut.training.repository.ProblemLikeRepository;
import com.whut.training.service.impl.ProblemLikeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProblemLikeServiceImplTest {

    @Mock
    private ProblemLikeRepository problemLikeRepository;

    @Mock
    private DailyProblemRepository dailyProblemRepository;

    private ProblemLikeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProblemLikeServiceImpl(problemLikeRepository, dailyProblemRepository);
    }

    @Test
    void likeProblemRejectsUnknownProblemKey() {
        User user = new User(1L, "alice", "alice@example.com", "password123", UserRole.USER);

        when(dailyProblemRepository.existsProblemKey("2999-Z")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.likeProblem(user, "2999-Z"));

        assertEquals(404, ex.getCode());
        verify(problemLikeRepository, never()).insertLike(user.getId(), "2999-Z");
    }

    @Test
    void repeatedLikeKeepsIdempotentSummary() {
        User user = new User(2L, "alice", "alice@example.com", "password123", UserRole.USER);
        ProblemLikeSummary summary = new ProblemLikeSummary("2000-A", 1, true);

        when(dailyProblemRepository.existsProblemKey("2000-A")).thenReturn(true);
        when(problemLikeRepository.findLikeStats(List.of("2000-A"), user.getId())).thenReturn(Map.of("2000-A", summary));

        ProblemLikeSummary response = service.likeProblem(user, "2000-A");

        assertEquals(1, response.likeCount());
        assertTrue(response.likedByMe());
        verify(problemLikeRepository).insertLike(user.getId(), "2000-A");
    }

    @Test
    void unlikeWithoutExistingLikeIsIdempotent() {
        User user = new User(3L, "alice", "alice@example.com", "password123", UserRole.USER);
        ProblemLikeSummary summary = new ProblemLikeSummary("2000-A", 0, false);

        when(dailyProblemRepository.existsProblemKey("2000-A")).thenReturn(true);
        when(problemLikeRepository.findLikeStats(List.of("2000-A"), user.getId())).thenReturn(Map.of("2000-A", summary));

        ProblemLikeSummary response = service.unlikeProblem(user, "2000-A");

        assertEquals(0, response.likeCount());
        assertFalse(response.likedByMe());
        verify(problemLikeRepository).deleteLike(user.getId(), "2000-A");
    }
}
