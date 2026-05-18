package com.whut.training.domain.dto;

public record ProblemFavoriteSummary(
        String problemKey,
        boolean favoritedByMe,
        String favoritedAt
) {
}
