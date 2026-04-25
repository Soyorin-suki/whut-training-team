package com.whut.training.domain.dto;

public record PracticeHistoryItem(
        Long drawId,
        String drawDate,
        String problemKey,
        String name,
        Integer rating,
        String sourceUrl,
        Long submissionId,
        String verdict,
        String checkedAt
) {
}
