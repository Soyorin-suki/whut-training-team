package com.whut.training.domain.dto;

import java.util.List;

public record DailyProblemCommentItem(
        Long id,
        String dailyProblemDate,
        String problemKey,
        String content,
        String createdAt,
        Long replyCommentId,
        String replyToUsername,
        DailyProblemCommentAuthor author,
        List<DailyProblemCommentItem> replies
) {
}
