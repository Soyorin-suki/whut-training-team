package com.whut.training.controller;

import com.whut.training.common.ApiResponse;
import com.whut.training.context.UserContext;
import com.whut.training.domain.dto.DailyProblemCommentArchiveItem;
import com.whut.training.domain.dto.DailyProblemCommentItem;
import com.whut.training.domain.dto.DailyProblemCommentRequest;
import com.whut.training.domain.entity.DailyProblem;
import com.whut.training.domain.entity.User;
import com.whut.training.exception.BusinessException;
import com.whut.training.service.DailyProblemCommentService;
import com.whut.training.service.DailyProblemService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/daily-problem/comments")
public class DailyProblemCommentController {

    private final DailyProblemCommentService dailyProblemCommentService;
    private final DailyProblemService dailyProblemService;

    public DailyProblemCommentController(DailyProblemCommentService dailyProblemCommentService,
                                         DailyProblemService dailyProblemService) {
        this.dailyProblemCommentService = dailyProblemCommentService;
        this.dailyProblemService = dailyProblemService;
    }

    @GetMapping("/today")
    public ApiResponse<List<DailyProblemCommentItem>> getTodayComments() {
        requireCurrentUser();
        DailyProblem dailyProblem = dailyProblemService.resolveTodayProblem();
        return ApiResponse.ok(
                dailyProblemCommentService.getComments(dailyProblem.date(), dailyProblem.problemKey())
        );
    }

    @GetMapping("/archives")
    public ApiResponse<List<DailyProblemCommentArchiveItem>> getCommentArchives(
            @RequestParam(defaultValue = "30") int limit) {
        requireCurrentUser();
        return ApiResponse.ok(dailyProblemCommentService.getCommentArchives(limit));
    }

    @GetMapping
    public ApiResponse<List<DailyProblemCommentItem>> getCommentsByInstance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String problemKey) {
        requireCurrentUser();
        return ApiResponse.ok(
                dailyProblemCommentService.getComments(date, problemKey)
        );
    }

    @PostMapping
    public ApiResponse<DailyProblemCommentItem> createComment(@Valid @RequestBody DailyProblemCommentRequest request) {
        User user = requireCurrentUser();
        DailyProblemTarget target = resolveDailyProblemTarget(request);
        return ApiResponse.ok(
                dailyProblemCommentService.createComment(user, target.date(), target.problemKey(), request)
        );
    }

    private DailyProblemTarget resolveDailyProblemTarget(DailyProblemCommentRequest request) {
        if (request != null && request.hasDailyProblemInstanceTarget()) {
            if (request.dailyProblemDate() == null) {
                throw new BusinessException(400, "dailyProblemDate is required when problemKey is provided");
            }
            if (request.problemKey() == null || request.problemKey().isBlank()) {
                throw new BusinessException(400, "problemKey is required when dailyProblemDate is provided");
            }
            return new DailyProblemTarget(request.dailyProblemDate(), request.problemKey().trim());
        }

        DailyProblem dailyProblem = dailyProblemService.resolveTodayProblem();
        return new DailyProblemTarget(dailyProblem.date(), dailyProblem.problemKey());
    }

    private User requireCurrentUser() {
        User user = UserContext.getCurrentUser();
        if (user == null) {
            throw new BusinessException(401, "unauthorized");
        }
        return user;
    }

    private record DailyProblemTarget(LocalDate date, String problemKey) {
    }
}
