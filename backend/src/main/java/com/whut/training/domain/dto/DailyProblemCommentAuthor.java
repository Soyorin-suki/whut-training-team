package com.whut.training.domain.dto;

public record DailyProblemCommentAuthor(
        Long userId,
        String username,
        String avatarUrl
) {
}
