package com.whut.training.domain.dto;

import java.util.List;

public record ProblemCommentItem(
        Long id,
        String problemKey,
        String content,
        String createdAt,
        Long replyCommentId,
        String replyToUsername,
        ProblemCommentAuthor author,
        List<ProblemCommentItem> replies
) {
}
