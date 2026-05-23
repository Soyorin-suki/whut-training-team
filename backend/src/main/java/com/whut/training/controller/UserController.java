package com.whut.training.controller;

import com.whut.training.common.ApiResponse;
import com.whut.training.context.UserContext;
import com.whut.training.domain.dto.DailyHeatmapItem;
import com.whut.training.domain.dto.UserUpdateRequest;
import com.whut.training.domain.dto.UserRegisterRequest;
import com.whut.training.domain.entity.User;
import com.whut.training.exception.BusinessException;
import com.whut.training.repository.DailyProblemRepository;
import com.whut.training.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户接口。
 *
 * <p>提供注册、用户列表、按 ID 查询以及当前用户资料修改能力。当前实现中，修改自己资料需要已登录。
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final DailyProblemRepository dailyProblemRepository;

    /**
     * 创建用户控制器。
     *
     * @param userService 用户服务。
     */
    public UserController(UserService userService, DailyProblemRepository dailyProblemRepository) {
        this.userService = userService;
        this.dailyProblemRepository = dailyProblemRepository;
    }

    /**
     * 注册新用户。
     *
     * @param request 注册请求体，包含 Codeforces handle、邮箱和密码。
     * @return 注册成功后的用户信息。
     */
    @PostMapping("/register")
    public ApiResponse<User> register(@Valid @RequestBody UserRegisterRequest request) {
        return ApiResponse.ok(userService.register(request));
    }

    /**
     * 获取全部用户列表。
     *
     * @return 按 ID 排序的用户列表。
     */
    @GetMapping
    public ApiResponse<List<User>> list() {
        return ApiResponse.ok(userService.list());
    }

    /**
     * 根据 ID 查询用户。
     *
     * <p>普通用户只能查看自己的信息，管理员可查看任意用户。
     *
     * @param id 用户 ID。
     * @return 对应用户信息。
     */
    @GetMapping("/{id}")
    public ApiResponse<User> getById(@PathVariable Long id) {
        User currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(401, "unauthorized");
        }
        if (!currentUser.getId().equals(id) && currentUser.getRole() != com.whut.training.domain.enums.UserRole.ADMIN) {
            throw new BusinessException(403, "forbidden");
        }
        return ApiResponse.ok(userService.getById(id));
    }

    /**
     * 修改当前登录用户资料。
     *
     * <p>该接口允许修改用户名、邮箱和密码；用户名更新会触发 Codeforces 账号校验并同步统计数据。
     *
     * @param request 资料更新请求体，允许为空。
     * @return 更新后的用户信息。
     */
    @PatchMapping("/{id}")
    public ApiResponse<User> updateById(@PathVariable Long id, @RequestBody(required = false) UserUpdateRequest request) {
        User currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(401, "unauthorized");
        }
        // 只有本人或管理员可以修改指定用户资料
        if (!currentUser.getId().equals(id) && currentUser.getRole() != com.whut.training.domain.enums.UserRole.ADMIN) {
            throw new BusinessException(403, "forbidden");
        }
        return ApiResponse.ok(userService.updateProfile(id, request));
    }

    /**
     * 获取用户每日提交热图数据。
     *
     * @param id   用户 ID。
     * @param days 查询天数，默认 180。
     * @return 热图数据列表。
     */
    @GetMapping("/{id}/daily-heatmap")
    public ApiResponse<List<DailyHeatmapItem>> dailyHeatmap(@PathVariable Long id, @RequestParam(defaultValue = "180") int days) {
        User currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(401, "unauthorized");
        }
        if (!currentUser.getId().equals(id) && currentUser.getRole() != com.whut.training.domain.enums.UserRole.ADMIN) {
            throw new BusinessException(403, "forbidden");
        }
        if (days < 1) days = 180;
        if (days > 365) days = 365;
        return ApiResponse.ok(dailyProblemRepository.findHeatmapForUser(id, days));
    }
}
