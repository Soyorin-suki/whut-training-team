package com.whut.training.controller;

import com.whut.training.common.ApiResponse;
import com.whut.training.context.UserContext;
import com.whut.training.domain.dto.LeaderboardEntryResponse;
import com.whut.training.domain.dto.LeaderboardPageResponse;
import com.whut.training.domain.entity.User;
import com.whut.training.exception.BusinessException;
import com.whut.training.service.RankService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rankings")
public class RankController {

    private final RankService rankService;

    public RankController(RankService rankService) {
        this.rankService = rankService;
    }

    @GetMapping
    public ApiResponse<LeaderboardPageResponse> list(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") String page,
            @RequestParam(defaultValue = "20") String pageSize
    ) {
        User currentUser = requireCurrentUser();
        return ApiResponse.ok(rankService.getLeaderboard(type, page, pageSize, currentUser.getId()));
    }

    @GetMapping("/me")
    public ApiResponse<LeaderboardEntryResponse> me(@RequestParam(required = false) String type) {
        return ApiResponse.ok(rankService.getMyLeaderboardEntry(type, requireCurrentUser().getId()));
    }

    private User requireCurrentUser() {
        User user = UserContext.getCurrentUser();
        if (user == null) {
            throw new BusinessException(401, "unauthorized");
        }
        return user;
    }
}
