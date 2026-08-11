package com.whut.training.controller;

import com.whut.training.common.ApiResponse;
import com.whut.training.domain.dto.UpcomingContestItem;
import com.whut.training.service.ContestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 近期比赛接口。
 */
@RestController
@RequestMapping("/api/contests")
public class ContestController {

    private final ContestService contestService;

    public ContestController(ContestService contestService) {
        this.contestService = contestService;
    }

    @GetMapping("/upcoming")
    public ApiResponse<List<UpcomingContestItem>> upcoming() {
        return ApiResponse.ok(contestService.getUpcomingContests());
    }
}
