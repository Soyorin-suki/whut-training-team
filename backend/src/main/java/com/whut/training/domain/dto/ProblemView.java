package com.whut.training.domain.dto;

public record ProblemView(
        String type,
        String date,
        String problemKey,
        Integer contestId,
        String problemIndex,
        String name,
        Integer rating,
        String tags,
        String sourceUrl,
        Integer likeCount,
        boolean likedByMe,
        boolean favoritedByMe,
        String favoritedAt
) {
}
