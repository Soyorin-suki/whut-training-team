package com.whut.training.service;

import com.whut.training.domain.dto.ProblemLikeSummary;
import com.whut.training.domain.entity.User;

import java.util.Collection;
import java.util.Map;

public interface ProblemLikeService {
    ProblemLikeSummary likeProblem(User user, String problemKey);

    ProblemLikeSummary unlikeProblem(User user, String problemKey);

    Map<String, ProblemLikeSummary> getLikeStats(Collection<String> problemKeys, Long userId);
}
