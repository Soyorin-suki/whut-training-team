package com.whut.training.controller;

import com.whut.training.common.ApiResponse;
import com.whut.training.context.UserContext;
import com.whut.training.domain.dto.ProblemLikeRequest;
import com.whut.training.domain.dto.ProblemLikeSummary;
import com.whut.training.domain.entity.User;
import com.whut.training.exception.BusinessException;
import com.whut.training.service.ProblemLikeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/problem-like")
public class ProblemLikeController {

    private final ProblemLikeService problemLikeService;

    public ProblemLikeController(ProblemLikeService problemLikeService) {
        this.problemLikeService = problemLikeService;
    }

    @PostMapping
    public ApiResponse<ProblemLikeSummary> likeProblem(@Valid @RequestBody ProblemLikeRequest request) {
        return ApiResponse.ok(problemLikeService.likeProblem(requireCurrentUser(), request.problemKey()));
    }

    @DeleteMapping("/{problemKey}")
    public ApiResponse<ProblemLikeSummary> unlikeProblem(@PathVariable String problemKey) {
        return ApiResponse.ok(problemLikeService.unlikeProblem(requireCurrentUser(), problemKey));
    }

    private User requireCurrentUser() {
        User user = UserContext.getCurrentUser();
        if (user == null) {
            throw new BusinessException(401, "unauthorized");
        }
        return user;
    }
}
