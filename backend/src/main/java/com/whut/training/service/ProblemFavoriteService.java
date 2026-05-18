package com.whut.training.service;

import com.whut.training.domain.dto.FavoriteProblemPageResponse;
import com.whut.training.domain.dto.ProblemFavoriteSummary;
import com.whut.training.domain.entity.User;

import java.util.Collection;
import java.util.Map;

public interface ProblemFavoriteService {
    ProblemFavoriteSummary favoriteProblem(User user, String problemKey);

    ProblemFavoriteSummary unfavoriteProblem(User user, String problemKey);

    Map<String, ProblemFavoriteSummary> getFavoriteStats(Collection<String> problemKeys, Long userId);

    FavoriteProblemPageResponse getMyFavorites(User user, int page, int limit);
}
