package com.whut.training.controller;

import com.whut.training.common.ApiResponse;
import com.whut.training.domain.dto.LeaderboardItem;
import com.whut.training.service.LeaderboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> top(
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "type", defaultValue = "total") String type) {
        if (limit < 1) limit = 10;
        if (limit > 100) limit = 100;
        if (page < 1) page = 1;

        int offset = (page - 1) * limit;
        List<LeaderboardItem> items = leaderboardService.getTop(limit, offset);
        int totalUsers = leaderboardService.countTotal();

        Map<String, Object> result = Map.of(
                "items", items,
                "total", totalUsers,
                "page", page,
                "limit", limit
        );
        return ApiResponse.ok(result);
    }
}
