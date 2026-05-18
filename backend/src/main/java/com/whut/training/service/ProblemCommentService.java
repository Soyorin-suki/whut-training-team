package com.whut.training.service;

import com.whut.training.domain.dto.ProblemCommentItem;
import com.whut.training.domain.dto.ProblemCommentRequest;
import com.whut.training.domain.entity.User;

import java.util.List;

public interface ProblemCommentService {
    List<ProblemCommentItem> getComments(String problemKey);

    ProblemCommentItem createComment(User user, ProblemCommentRequest request);
}
