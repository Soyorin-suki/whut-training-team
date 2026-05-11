package com.whut.training.domain.dto;

import com.whut.training.domain.enums.UserRole;

import java.util.List;

public record AdminUserTimelineResponse(
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
        List<AdminUserTimelineItem> entries
) {
}
