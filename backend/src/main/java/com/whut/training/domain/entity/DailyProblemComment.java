package com.whut.training.domain.entity;

import java.time.LocalDate;

public record DailyProblemComment(
        Long id,
        LocalDate dailyProblemDate,
        String problemKey,
        Long userId,
        Long replyCommentId,
        String content,
        String createdAt
) {
}
