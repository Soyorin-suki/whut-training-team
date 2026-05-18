package com.whut.training.service.impl;

import com.whut.training.aspect.annotation.ServiceLog;
import com.whut.training.domain.dto.FavoriteProblemPageResponse;
import com.whut.training.domain.dto.ProblemFavoriteSummary;
import com.whut.training.domain.entity.User;
import com.whut.training.exception.BusinessException;
import com.whut.training.repository.DailyProblemRepository;
import com.whut.training.repository.ProblemFavoriteRepository;
import com.whut.training.service.ProblemFavoriteService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@ServiceLog
public class ProblemFavoriteServiceImpl implements ProblemFavoriteService {

    private final ProblemFavoriteRepository problemFavoriteRepository;
    private final DailyProblemRepository dailyProblemRepository;

    public ProblemFavoriteServiceImpl(ProblemFavoriteRepository problemFavoriteRepository,
                                      DailyProblemRepository dailyProblemRepository) {
        this.problemFavoriteRepository = problemFavoriteRepository;
        this.dailyProblemRepository = dailyProblemRepository;
    }

    @Override
    public ProblemFavoriteSummary favoriteProblem(User user, String problemKey) {
        String normalizedProblemKey = normalizeProblemKey(problemKey);
        validateProblemExists(normalizedProblemKey);
        problemFavoriteRepository.insertFavorite(user.getId(), normalizedProblemKey);
        return getFavoriteSummary(normalizedProblemKey, user.getId());
    }

    @Override
    public ProblemFavoriteSummary unfavoriteProblem(User user, String problemKey) {
        String normalizedProblemKey = normalizeProblemKey(problemKey);
        validateProblemExists(normalizedProblemKey);
        problemFavoriteRepository.deleteFavorite(user.getId(), normalizedProblemKey);
        return getFavoriteSummary(normalizedProblemKey, user.getId());
    }

    @Override
    public Map<String, ProblemFavoriteSummary> getFavoriteStats(Collection<String> problemKeys, Long userId) {
        if (problemKeys == null || problemKeys.isEmpty()) {
            return Map.of();
        }
        List<String> safeProblemKeys = new ArrayList<>(problemKeys.stream()
                .filter(problemKey -> problemKey != null && !problemKey.isBlank())
                .toList());
        if (safeProblemKeys.isEmpty()) {
            return Map.of();
        }
        return problemFavoriteRepository.findFavoriteStats(safeProblemKeys, userId);
    }

    @Override
    public FavoriteProblemPageResponse getMyFavorites(User user, int page, int limit) {
        int safePage = Math.max(1, page);
        int safeLimit = Math.max(1, Math.min(100, limit));
        return new FavoriteProblemPageResponse(
                problemFavoriteRepository.findUserFavorites(user.getId(), safePage, safeLimit),
                safePage,
                safeLimit,
                problemFavoriteRepository.countUserFavorites(user.getId())
        );
    }

    private ProblemFavoriteSummary getFavoriteSummary(String problemKey, Long userId) {
        return getFavoriteStats(List.of(problemKey), userId)
                .getOrDefault(problemKey, new ProblemFavoriteSummary(problemKey, false, null));
    }

    private String normalizeProblemKey(String problemKey) {
        if (problemKey == null || problemKey.isBlank()) {
            throw new BusinessException(400, "problemKey is required");
        }
        return problemKey.trim();
    }

    private void validateProblemExists(String problemKey) {
        if (!dailyProblemRepository.existsProblemKey(problemKey)) {
            throw new BusinessException(404, "problem not found");
        }
    }
}
