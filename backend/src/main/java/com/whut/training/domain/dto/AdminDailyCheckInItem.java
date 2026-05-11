package com.whut.training.domain.dto;

public record AdminDailyCheckInItem(
        Long userId,
        String username,
        String avatarUrl,
        Long submissionId,
        String verdict,
        Integer score,
        String checkedAt
) {
}
