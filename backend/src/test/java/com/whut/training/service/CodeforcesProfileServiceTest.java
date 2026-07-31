package com.whut.training.service;

import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.repository.CodeforcesProfileSnapshotRepository;
import com.whut.training.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CodeforcesProfileServiceTest {

    @Test
    void aggregatesUniqueSolvedProblemsTagsAndRecentRatingChanges() {
        UserRepository userRepository = mock(UserRepository.class);
        CodeforcesApiService apiService = mock(CodeforcesApiService.class);
        CodeforcesProfileSnapshotRepository snapshotRepository =
                mock(CodeforcesProfileSnapshotRepository.class);
        User user = new User(7L, "login-account", null, "password", UserRole.USER);
        user.setCodeforcesHandle("tourist");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        when(apiService.getUserSubmissions("tourist", 10000)).thenReturn(Optional.of(List.of(
                submission(1L, 100, "A", "OK", List.of("dp", "math"), 400L),
                submission(2L, 100, "A", "OK", List.of("dp", "math"), 300L),
                submission(3L, 101, "B", "WRONG_ANSWER", List.of("greedy"), 200L),
                submission(4L, 102, "C", "OK", List.of("dp"), 100L)
        )));
        when(apiService.getUserRatingHistory("tourist")).thenReturn(Optional.of(List.of(
                rating(1L, "Round 1", 1200, 1300, 10L),
                rating(2L, "Round 2", 1300, 1280, 20L)
        )));
        when(apiService.getUserInfo("tourist")).thenReturn(Optional.of(
                new CodeforcesApiService.CodeforcesUserProfile(1280, 1300, false, 500L, null)
        ));

        CodeforcesProfileService service =
                new CodeforcesProfileService(userRepository, apiService, snapshotRepository, 30);
        var overview = service.refresh(7L);

        assertThat(overview.solvedCount()).isEqualTo(2);
        assertThat(overview.attemptedCount()).isEqualTo(3);
        assertThat(overview.acceptedSubmissionCount()).isEqualTo(3);
        assertThat(overview.lastSubmissionAtSeconds()).isEqualTo(400L);
        assertThat(overview.tagStats())
                .extracting(stat -> stat.tag() + ":" + stat.count())
                .containsExactly("dp:2", "math:1");
        assertThat(overview.recentContests())
                .extracting(contest -> contest.contestName() + ":" + contest.ratingChange())
                .containsExactly("Round 2:-20", "Round 1:100");
        verify(snapshotRepository).save(eq(7L), eq("tourist"), any());
    }

    private CodeforcesApiService.CodeforcesSubmission submission(
            Long id,
            int contestId,
            String index,
            String verdict,
            List<String> tags,
            long time
    ) {
        return new CodeforcesApiService.CodeforcesSubmission(
                id, contestId, index, "Problem " + index, 1400, tags, verdict, time
        );
    }

    private CodeforcesApiService.CodeforcesRatingChange rating(
            Long contestId,
            String name,
            int oldRating,
            int newRating,
            long time
    ) {
        return new CodeforcesApiService.CodeforcesRatingChange(
                contestId, name, 100, oldRating, newRating, time
        );
    }
}
