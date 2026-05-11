package com.whut.training.domain.dto;

import com.whut.training.domain.enums.UserRole;

public record AdminUserTrainingItem(
        Long userId,
        String username,
        String email,
        UserRole role,
        String avatarUrl,
        Integer score,
        Integer solvedProblemCount,
        Integer hardSolvedProblemCount,
        Integer currentStreakDays,
        Integer longestStreakDays,
        long dailyCheckInCount,
        long practiceCheckCount,
        String lastDailyDate,
        String lastPracticeCheckedAt
) {
}
