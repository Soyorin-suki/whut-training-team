package com.whut.training.controller;

import com.whut.training.common.ApiResponse;
import com.whut.training.context.UserContext;
import com.whut.training.domain.dto.AdminCreateUserRequest;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.exception.BusinessException;
import com.whut.training.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ApiResponse<User> createByAdmin(@Valid @RequestBody AdminCreateUserRequest request) {
        User currentUser = UserContext.getCurrentUser();
        if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
            throw new BusinessException(403, "admin role required");
        }
        return ApiResponse.ok(userService.createByAdmin(request));
    }
}
