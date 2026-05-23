package com.whut.training.controller;

import com.whut.training.common.ApiResponse;
import com.whut.training.context.UserContext;
import com.whut.training.domain.entity.PushPoolItem;
import com.whut.training.domain.entity.PushSubmission;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.exception.BusinessException;
import com.whut.training.service.PushPoolService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PushPoolController {

    private final PushPoolService pushPoolService;

    public PushPoolController(PushPoolService pushPoolService) {
        this.pushPoolService = pushPoolService;
    }

    @PostMapping("/push")
    public ApiResponse<PushPoolItem> submit(@RequestBody Map<String, String> body) {
        User user = requireCurrentUser();
        return ApiResponse.ok(pushPoolService.submit(user,
                body.get("title"), body.get("link"), body.get("description")));
    }

    @GetMapping("/push")
    public ApiResponse<List<PushPoolItem>> list() {
        User user = requireCurrentUser();
        return ApiResponse.ok(pushPoolService.list(user));
    }

    @PostMapping("/push/{id}/submit")
    public ApiResponse<PushSubmission> submitSolution(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = requireCurrentUser();
        return ApiResponse.ok(pushPoolService.submitSolution(user, id,
                body.get("submissionLink"), body.get("resultDescription")));
    }

    @GetMapping("/push/{id}/submissions")
    public ApiResponse<List<PushSubmission>> getSubmissions(@PathVariable Long id) {
        User user = requireCurrentUser();
        return ApiResponse.ok(pushPoolService.getSubmissions(user, id));
    }

    @PostMapping("/admin/push/{id}/approve")
    public ApiResponse<PushPoolItem> approve(@PathVariable Long id) {
        User user = requireAdminUser();
        return ApiResponse.ok(pushPoolService.approve(user, id));
    }

    @PostMapping("/admin/push/{id}/reject")
    public ApiResponse<PushPoolItem> reject(@PathVariable Long id) {
        User user = requireAdminUser();
        return ApiResponse.ok(pushPoolService.reject(user, id));
    }

    @PostMapping("/admin/push/{id}/promote")
    public ApiResponse<PushPoolItem> promote(@PathVariable Long id) {
        User user = requireAdminUser();
        return ApiResponse.ok(pushPoolService.promote(user, id));
    }

    private User requireCurrentUser() {
        User user = UserContext.getCurrentUser();
        if (user == null) {
            throw new BusinessException(401, "unauthorized");
        }
        return user;
    }

    private User requireAdminUser() {
        User user = requireCurrentUser();
        if (user.getRole() != UserRole.ADMIN) {
            throw new BusinessException(403, "admin role required");
        }
        return user;
    }
}
