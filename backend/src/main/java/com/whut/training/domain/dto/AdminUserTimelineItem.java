package com.whut.training.domain.dto;

public record AdminUserTimelineItem(
        String activityType,
        String activityAt,
        String activityDate,
        Long drawId,
        String problemKey,
        String name,
        Integer rating,
        String sourceUrl,
        Long submissionId,
        String verdict,
        Integer score
) {
}
