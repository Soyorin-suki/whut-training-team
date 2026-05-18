package com.whut.training.service;

import com.whut.training.domain.dto.ProblemDetailView;
import com.whut.training.domain.entity.User;

public interface ProblemDetailService {
    ProblemDetailView getProblemDetail(String problemKey, User currentUser);
}
