package com.whut.training.domain.dto;

import com.whut.training.domain.enums.LeaderboardType;

import java.util.List;

public record LeaderboardPageResponse(
        LeaderboardType type,
        int page,
        int pageSize,
        long total,
        List<LeaderboardEntryResponse> entries,
        LeaderboardEntryResponse currentUserEntry
) {
}
