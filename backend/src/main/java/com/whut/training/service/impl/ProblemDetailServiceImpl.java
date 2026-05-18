package com.whut.training.service.impl;

import com.whut.training.aspect.annotation.ServiceLog;
import com.whut.training.domain.dto.ProblemDetailView;
import com.whut.training.domain.dto.ProblemFavoriteSummary;
import com.whut.training.domain.dto.ProblemLikeSummary;
import com.whut.training.domain.entity.CfProblem;
import com.whut.training.domain.entity.User;
import com.whut.training.exception.BusinessException;
import com.whut.training.repository.DailyProblemRepository;
import com.whut.training.service.ProblemDetailService;
import com.whut.training.service.ProblemFavoriteService;
import com.whut.training.service.ProblemLikeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ServiceLog
public class ProblemDetailServiceImpl implements ProblemDetailService {

    private final DailyProblemRepository dailyProblemRepository;
    private final ProblemLikeService problemLikeService;
    private final ProblemFavoriteService problemFavoriteService;

    public ProblemDetailServiceImpl(DailyProblemRepository dailyProblemRepository,
                                    ProblemLikeService problemLikeService,
                                    ProblemFavoriteService problemFavoriteService) {
        this.dailyProblemRepository = dailyProblemRepository;
        this.problemLikeService = problemLikeService;
        this.problemFavoriteService = problemFavoriteService;
    }

    @Override
    public ProblemDetailView getProblemDetail(String problemKey, User currentUser) {
        String normalizedProblemKey = normalizeProblemKey(problemKey);
        CfProblem problem = dailyProblemRepository.findProblemByKey(normalizedProblemKey)
                .orElseThrow(() -> new BusinessException(404, "problem not found"));
        Long userId = currentUser == null ? null : currentUser.getId();
        ProblemLikeSummary likeSummary = problemLikeService.getLikeStats(List.of(normalizedProblemKey), userId)
                .getOrDefault(normalizedProblemKey, new ProblemLikeSummary(normalizedProblemKey, 0, false));
        ProblemFavoriteSummary favoriteSummary =
                problemFavoriteService.getFavoriteStats(List.of(normalizedProblemKey), userId)
                        .getOrDefault(normalizedProblemKey, new ProblemFavoriteSummary(normalizedProblemKey, false, null));
        return new ProblemDetailView(
                problem.problemKey(),
                problem.contestId(),
                problem.problemIndex(),
                problem.name(),
                problem.rating(),
                problem.tags(),
                problem.sourceUrl(),
                likeSummary.likeCount(),
                likeSummary.likedByMe(),
                favoriteSummary.favoritedByMe(),
                favoriteSummary.favoritedAt()
        );
    }

    private String normalizeProblemKey(String problemKey) {
        if (problemKey == null || problemKey.isBlank()) {
            throw new BusinessException(400, "problemKey is required");
        }
        return problemKey.trim();
    }
}
