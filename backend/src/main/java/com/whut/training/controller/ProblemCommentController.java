package com.whut.training.controller;

import com.whut.training.common.ApiResponse;
import com.whut.training.context.UserContext;
import com.whut.training.domain.dto.ProblemCommentItem;
import com.whut.training.domain.dto.ProblemCommentRequest;
import com.whut.training.domain.entity.User;
import com.whut.training.exception.BusinessException;
import com.whut.training.service.ProblemCommentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/problem-comments")
public class ProblemCommentController {

    private final ProblemCommentService problemCommentService;

    public ProblemCommentController(ProblemCommentService problemCommentService) {
        this.problemCommentService = problemCommentService;
    }

    @GetMapping("/{problemKey}")
    public ApiResponse<List<ProblemCommentItem>> getProblemComments(@PathVariable String problemKey) {
        return ApiResponse.ok(problemCommentService.getComments(problemKey));
    }

    @PostMapping
    public ApiResponse<ProblemCommentItem> createProblemComment(@Valid @RequestBody ProblemCommentRequest request) {
        return ApiResponse.ok(problemCommentService.createComment(requireCurrentUser(), request));
    }

    private User requireCurrentUser() {
        User user = UserContext.getCurrentUser();
        if (user == null) {
            throw new BusinessException(401, "unauthorized");
        }
        return user;
    }
}
