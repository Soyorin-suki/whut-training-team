package com.whut.training.controller;

import com.whut.training.common.ApiResponse;
import com.whut.training.context.UserContext;
import com.whut.training.domain.dto.ProblemView;
import com.whut.training.domain.entity.User;
import com.whut.training.exception.BusinessException;
import com.whut.training.service.DailyProblemService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/daily-problem")
public class AdminDailyProblemController {

    private final DailyProblemService dailyProblemService;

    public AdminDailyProblemController(DailyProblemService dailyProblemService) {
        this.dailyProblemService = dailyProblemService;
    }

    @PostMapping("/regenerate")
    public ApiResponse<ProblemView> regenerateToday() {
        User user = UserContext.getCurrentUser();
        if (user == null) {
            throw new BusinessException(401, "unauthorized");
        }
        return ApiResponse.ok(dailyProblemService.regenerateTodayByAdmin(user));
    }
}
