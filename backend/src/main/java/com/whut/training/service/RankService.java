package com.whut.training.service;

import com.whut.training.domain.dto.LeaderboardEntryResponse;
import com.whut.training.domain.dto.LeaderboardPageResponse;

public interface RankService {
    LeaderboardPageResponse getLeaderboard(String type, String page, String pageSize, Long currentUserId);

    LeaderboardEntryResponse getMyLeaderboardEntry(String type, Long currentUserId);
}
