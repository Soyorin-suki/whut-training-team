package com.whut.training.controller;

import com.whut.training.common.ApiResponse;
import com.whut.training.context.UserContext;
import com.whut.training.domain.dto.*;
import com.whut.training.domain.entity.User;
import com.whut.training.exception.BusinessException;
import com.whut.training.service.DailyProblemService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DailyProblemController {

    private final DailyProblemService dailyProblemService;

    public DailyProblemController(DailyProblemService dailyProblemService) {
        this.dailyProblemService = dailyProblemService;
    }

    @GetMapping("/daily-problem/today")
    public ApiResponse<DailyProblemTodayResponse> getToday() {
        return ApiResponse.ok(dailyProblemService.getToday(requireCurrentUser()));
    }

    @PostMapping("/daily-problem/check-in")
    public ApiResponse<CheckInResultResponse> checkIn(@Valid @RequestBody DailyProblemCheckInRequest request) {
        return ApiResponse.ok(dailyProblemService.checkIn(requireCurrentUser(), request.submissionId()));
    }

    @GetMapping("/daily-problem/history")
    public ApiResponse<List<DailyProblemHistoryItem>> history(@RequestParam(defaultValue = "14") int limit) {
        return ApiResponse.ok(dailyProblemService.getHistory(requireCurrentUser(), limit));
    }

    @PostMapping("/practice/draw")
    public ApiResponse<PracticeDrawResponse> draw(@RequestBody(required = false) PracticeDrawRequest request) {
        Integer minRating = request == null ? null : request.minRating();
        Integer maxRating = request == null ? null : request.maxRating();
        return ApiResponse.ok(dailyProblemService.drawPracticeProblem(requireCurrentUser(), minRating, maxRating));
    }

    @PostMapping("/practice/check")
    public ApiResponse<CheckInResultResponse> checkPractice(@Valid @RequestBody PracticeCheckRequest request) {
        return ApiResponse.ok(
                dailyProblemService.checkPractice(requireCurrentUser(), request.drawId(), request.submissionId())
        );
    }

    private User requireCurrentUser() {
        User user = UserContext.getCurrentUser();
        if (user == null) {
            throw new BusinessException(401, "unauthorized");
        }
        return user;
    }
}
