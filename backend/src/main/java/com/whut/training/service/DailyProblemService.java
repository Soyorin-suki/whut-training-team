package com.whut.training.service;

import com.whut.training.domain.dto.*;
import com.whut.training.domain.entity.User;

import java.util.List;

public interface DailyProblemService {
    DailyProblemTodayResponse getToday(User user);

    CheckInResultResponse checkIn(User user, Long submissionId);

    List<DailyProblemHistoryItem> getHistory(User user, int limit);

    PracticeDrawResponse drawPracticeProblem(User user, Integer minRating, Integer maxRating);

    CheckInResultResponse checkPractice(User user, Long drawId, Long submissionId);

    ProblemView regenerateTodayByAdmin(User adminUser);
}
