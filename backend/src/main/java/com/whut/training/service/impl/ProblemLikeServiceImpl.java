package com.whut.training.service.impl;

import com.whut.training.aspect.annotation.ServiceLog;
import com.whut.training.domain.dto.ProblemLikeSummary;
import com.whut.training.domain.entity.User;
import com.whut.training.exception.BusinessException;
import com.whut.training.repository.DailyProblemRepository;
import com.whut.training.repository.ProblemLikeRepository;
import com.whut.training.service.ProblemLikeService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@ServiceLog
public class ProblemLikeServiceImpl implements ProblemLikeService {

    private final ProblemLikeRepository problemLikeRepository;
    private final DailyProblemRepository dailyProblemRepository;

    public ProblemLikeServiceImpl(ProblemLikeRepository problemLikeRepository,
                                  DailyProblemRepository dailyProblemRepository) {
        this.problemLikeRepository = problemLikeRepository;
        this.dailyProblemRepository = dailyProblemRepository;
    }

    @Override
    public ProblemLikeSummary likeProblem(User user, String problemKey) {
        String normalizedProblemKey = normalizeProblemKey(problemKey);
        validateProblemExists(normalizedProblemKey);
        problemLikeRepository.insertLike(user.getId(), normalizedProblemKey);
        return getLikeSummary(normalizedProblemKey, user.getId());
    }

    @Override
    public ProblemLikeSummary unlikeProblem(User user, String problemKey) {
        String normalizedProblemKey = normalizeProblemKey(problemKey);
        validateProblemExists(normalizedProblemKey);
        problemLikeRepository.deleteLike(user.getId(), normalizedProblemKey);
        return getLikeSummary(normalizedProblemKey, user.getId());
    }

    @Override
    public Map<String, ProblemLikeSummary> getLikeStats(Collection<String> problemKeys, Long userId) {
        if (problemKeys == null || problemKeys.isEmpty()) {
            return Map.of();
        }
        List<String> safeProblemKeys = new ArrayList<>(problemKeys.stream()
                .filter(problemKey -> problemKey != null && !problemKey.isBlank())
                .toList());
        if (safeProblemKeys.isEmpty()) {
            return Map.of();
        }
        return problemLikeRepository.findLikeStats(safeProblemKeys, userId);
    }

    private ProblemLikeSummary getLikeSummary(String problemKey, Long userId) {
        return getLikeStats(List.of(problemKey), userId)
                .getOrDefault(problemKey, new ProblemLikeSummary(problemKey, 0, false));
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
