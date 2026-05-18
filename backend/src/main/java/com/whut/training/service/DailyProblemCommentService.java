package com.whut.training.service;

import com.whut.training.domain.dto.DailyProblemCommentArchiveItem;
import com.whut.training.domain.dto.DailyProblemCommentItem;
import com.whut.training.domain.dto.DailyProblemCommentRequest;
import com.whut.training.domain.entity.User;

import java.time.LocalDate;
import java.util.List;

public interface DailyProblemCommentService {
    List<DailyProblemCommentArchiveItem> getCommentArchives(int limit);

    List<DailyProblemCommentItem> getComments(LocalDate dailyProblemDate, String problemKey);

    DailyProblemCommentItem createComment(User user, LocalDate dailyProblemDate, String problemKey,
                                          DailyProblemCommentRequest request);
}
