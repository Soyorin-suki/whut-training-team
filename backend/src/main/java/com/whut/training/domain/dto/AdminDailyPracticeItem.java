package com.whut.training.domain.dto;

public record AdminDailyPracticeItem(
        Long drawId,
        Long userId,
        String username,
        String avatarUrl,
        String problemKey,
        Integer contestId,
        String problemIndex,
        String name,
        Integer rating,
        String tags,
        String sourceUrl,
        Long submissionId,
        String verdict,
        String checkedAt
) {
}
