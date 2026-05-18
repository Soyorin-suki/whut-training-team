package com.whut.training.controller;

import com.whut.training.common.ApiResponse;
import com.whut.training.context.UserContext;
import com.whut.training.domain.dto.FavoriteProblemPageResponse;
import com.whut.training.domain.dto.ProblemFavoriteRequest;
import com.whut.training.domain.dto.ProblemFavoriteSummary;
import com.whut.training.domain.entity.User;
import com.whut.training.exception.BusinessException;
import com.whut.training.service.ProblemFavoriteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/problem-favorite")
public class ProblemFavoriteController {

    private final ProblemFavoriteService problemFavoriteService;

    public ProblemFavoriteController(ProblemFavoriteService problemFavoriteService) {
        this.problemFavoriteService = problemFavoriteService;
    }

    @PostMapping
    public ApiResponse<ProblemFavoriteSummary> favoriteProblem(@Valid @RequestBody ProblemFavoriteRequest request) {
        return ApiResponse.ok(problemFavoriteService.favoriteProblem(requireCurrentUser(), request.problemKey()));
    }

    @DeleteMapping("/{problemKey}")
    public ApiResponse<ProblemFavoriteSummary> unfavoriteProblem(@PathVariable String problemKey) {
        return ApiResponse.ok(problemFavoriteService.unfavoriteProblem(requireCurrentUser(), problemKey));
    }

    @GetMapping("/mine")
    public ApiResponse<FavoriteProblemPageResponse> getMyFavorites(@RequestParam(defaultValue = "1") int page,
                                                                   @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(problemFavoriteService.getMyFavorites(requireCurrentUser(), page, limit));
    }

    private User requireCurrentUser() {
        User user = UserContext.getCurrentUser();
        if (user == null) {
            throw new BusinessException(401, "unauthorized");
        }
        return user;
    }
}
