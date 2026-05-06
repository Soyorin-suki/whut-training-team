package com.whut.training.domain.dto;

public record LeaderboardEntryResponse(
        Integer rank,
        Long userId,
        String username,
        String avatarUrl,
        Integer score,
        boolean isCurrentUser
) {
}
