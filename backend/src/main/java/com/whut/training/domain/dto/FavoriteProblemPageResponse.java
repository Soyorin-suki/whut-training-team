package com.whut.training.domain.dto;

import java.util.List;

public record FavoriteProblemPageResponse(
        List<FavoriteProblemItem> items,
        int page,
        int limit,
        long total
) {
}
