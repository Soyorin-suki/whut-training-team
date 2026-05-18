package com.whut.training.domain.entity;

public record ProblemComment(
        Long id,
        String problemKey,
        Long userId,
        Long replyCommentId,
        String content,
        String createdAt,
        Long legacyCommentId
) {
}
