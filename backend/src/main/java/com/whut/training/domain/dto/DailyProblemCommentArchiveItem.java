package com.whut.training.domain.dto;

public record DailyProblemCommentArchiveItem(
        String dailyProblemDate,
        String problemKey,
        String name,
        Integer rating,
        String sourceUrl,
        Integer commentCount,
        String lastCommentedAt
) {
}
