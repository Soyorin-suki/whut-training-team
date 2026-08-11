package com.whut.training.domain.dto;

import java.util.List;

public record AtCoderAbcDashboard(
        TrackingSetting setting,
        Contest currentContest,
        List<Contest> contests,
        Summary summary,
        List<MemberStatus> members
) {
    public record TrackingSetting(int minimumAcCount, int graceHours) {}
    public record Contest(String contestId, String name, long startTimeSeconds, long endTimeSeconds,
                          String contestUrl, String syncStatus, String lastSyncAt) {}
    public record Summary(int requiredCount, int completedCount, int participatedCount, int absentCount,
                          int unboundCount, int exemptedCount, int dataErrorCount, double completionRate) {}
    public record MemberStatus(Long userId, String username, String displayName, String avatarUrl,
                               String atcoderHandle, boolean exempted, String exemptionReason,
                               String status, boolean participated, Integer acCount, List<String> solvedProblemIds,
                               Integer contestRank, Integer performance, Boolean rated,
                               Integer oldRating, Integer newRating, String checkedAt, String sourceError) {}
}
