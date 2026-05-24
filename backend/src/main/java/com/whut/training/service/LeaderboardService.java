package com.whut.training.service;

import com.whut.training.domain.dto.LeaderboardItem;

import java.util.List;

public interface LeaderboardService {
    List<LeaderboardItem> getTop(int limit, int offset);
    int countTotal();
}
