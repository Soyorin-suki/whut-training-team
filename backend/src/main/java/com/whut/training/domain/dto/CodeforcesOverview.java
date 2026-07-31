package com.whut.training.domain.dto;

import java.time.Instant;
import java.util.List;

/**
 * 用户的 Codeforces 公开统计快照。
 */
public record CodeforcesOverview(
        String handle,
        Integer currentRating,
        Integer maxRating,
        int solvedCount,
        int attemptedCount,
        int acceptedSubmissionCount,
        int ratedContestCount,
        Long lastSubmissionAtSeconds,
        boolean submissionLimitReached,
        Instant syncedAt,
        boolean stale,
        List<TagStat> tagStats,
        List<RecentContest> recentContests
) {
    public CodeforcesOverview withStale(boolean nextStale) {
        return new CodeforcesOverview(
                handle,
                currentRating,
                maxRating,
                solvedCount,
                attemptedCount,
                acceptedSubmissionCount,
                ratedContestCount,
                lastSubmissionAtSeconds,
                submissionLimitReached,
                syncedAt,
                nextStale,
                tagStats,
                recentContests
        );
    }

    public record TagStat(String tag, int count) {
    }

    public record RecentContest(
            Long contestId,
            String contestName,
            Integer rank,
            Integer oldRating,
            Integer newRating,
            Integer ratingChange,
            Long ratingUpdateTimeSeconds,
            String url
    ) {
    }
}
