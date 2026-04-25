package com.whut.training.domain.dto;

public record DailyProblemHistoryItem(
        String date,
        String problemKey,
        String name,
        Integer rating,
        String sourceUrl,
        boolean checkedIn,
        Long submissionId,
        String verdict,
        Integer score
) {
}
