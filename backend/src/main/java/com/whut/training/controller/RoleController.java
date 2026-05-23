package com.whut.training.controller;

import com.whut.training.common.ApiResponse;
import com.whut.training.context.UserContext;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.exception.BusinessException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @GetMapping
    public ApiResponse<List<RoleInfo>> list() {
        User currentUser = UserContext.getCurrentUser();
        if (currentUser == null || (currentUser.getRole() != UserRole.ADMIN && currentUser.getRole() != UserRole.SUPER_ADMIN)) {
            throw new BusinessException(403, "admin role required");
        }
        List<RoleInfo> roles = Arrays.stream(UserRole.values())
                .map(r -> new RoleInfo(r.name(), r.name(), r.name()))
                .toList();
        return ApiResponse.ok(roles);
    }

    public record RoleInfo(String name, String code, String description) {
    }
}
