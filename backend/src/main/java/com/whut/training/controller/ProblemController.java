package com.whut.training.controller;

import com.whut.training.common.ApiResponse;
import com.whut.training.context.UserContext;
import com.whut.training.domain.dto.ProblemDetailView;
import com.whut.training.service.ProblemDetailService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemDetailService problemDetailService;

    public ProblemController(ProblemDetailService problemDetailService) {
        this.problemDetailService = problemDetailService;
    }

    @GetMapping("/{problemKey}")
    public ApiResponse<ProblemDetailView> getProblemDetail(@PathVariable String problemKey) {
        return ApiResponse.ok(problemDetailService.getProblemDetail(problemKey, UserContext.getCurrentUser()));
    }
}
