package com.whut.training.domain.dto;

public record ProblemCommentAuthor(
        Long userId,
        String username,
        String avatarUrl
) {
}
