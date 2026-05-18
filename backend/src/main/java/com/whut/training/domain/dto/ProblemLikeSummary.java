package com.whut.training.domain.dto;

public record ProblemLikeSummary(
        String problemKey,
        Integer likeCount,
        boolean likedByMe
) {
}
