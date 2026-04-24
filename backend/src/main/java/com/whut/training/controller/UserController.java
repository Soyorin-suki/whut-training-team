package com.whut.training.controller;

import com.whut.training.common.ApiResponse;
import com.whut.training.domain.dto.UserRegisterRequest;
import com.whut.training.domain.entity.User;
import com.whut.training.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<User> register(@Valid @RequestBody UserRegisterRequest request) {
        return ApiResponse.ok(userService.register(request));
    }

    @GetMapping
    public ApiResponse<List<User>> list() {
        return ApiResponse.ok(userService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<User> getById(@PathVariable Long id) {
        return ApiResponse.ok(userService.getById(id));
    }
}
