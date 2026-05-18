package com.whut.training.domain.dto;

public record FavoriteProblemItem(
        String problemKey,
        Integer contestId,
        String problemIndex,
        String name,
        Integer rating,
        String tags,
        String sourceUrl,
        String sourceType,
        String favoritedAt
) {
}
