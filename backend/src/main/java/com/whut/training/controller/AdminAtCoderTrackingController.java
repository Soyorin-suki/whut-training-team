package com.whut.training.controller;

import com.whut.training.common.ApiResponse;
import com.whut.training.context.UserContext;
import com.whut.training.domain.dto.AtCoderAbcDashboard;
import com.whut.training.domain.dto.AtCoderExemptionRequest;
import com.whut.training.domain.dto.AtCoderTrackingSettingRequest;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.exception.BusinessException;
import com.whut.training.service.AtCoderTrackingUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/atcoder-abc")
public class AdminAtCoderTrackingController {
    private final AtCoderTrackingUseCase trackingService;

    public AdminAtCoderTrackingController(AtCoderTrackingUseCase trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping
    public ApiResponse<AtCoderAbcDashboard> dashboard(@RequestParam(required = false) String contestId) {
        requireAdmin();
        return ApiResponse.ok(trackingService.getDashboard(contestId));
    }

    @PostMapping("/refresh")
    public ApiResponse<AtCoderAbcDashboard> refresh(@RequestParam(required = false) String contestId) {
        requireAdmin();
        return ApiResponse.ok(trackingService.refreshAndGet(contestId));
    }

    @PatchMapping("/setting")
    public ApiResponse<AtCoderAbcDashboard> updateSetting(
            @Valid @RequestBody AtCoderTrackingSettingRequest request,
            @RequestParam(required = false) String contestId
    ) {
        User administrator = requireAdmin();
        return ApiResponse.ok(trackingService.updateSetting(request, administrator.getId(), contestId));
    }

    @PatchMapping("/{contestId}/members/{userId}/exemption")
    public ApiResponse<AtCoderAbcDashboard> exemption(
            @PathVariable String contestId,
            @PathVariable Long userId,
            @Valid @RequestBody AtCoderExemptionRequest request
    ) {
        requireAdmin();
        return ApiResponse.ok(trackingService.setExemption(contestId, userId, request));
    }

    private User requireAdmin() {
        User user = UserContext.getCurrentUser();
        if (user == null) throw new BusinessException(401, "unauthorized");
        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.SUPER_ADMIN) {
            throw new BusinessException(403, "admin role required");
        }
        return user;
    }
}
