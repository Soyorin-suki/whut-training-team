package com.whut.training.controller;

import com.whut.training.common.ApiResponse;
import com.whut.training.domain.dto.LoginRequest;
import com.whut.training.domain.dto.LoginResponse;
import com.whut.training.service.AuthService;
import com.whut.training.utils.TokenUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authorization,
                                    @RequestHeader("X-Refresh-Token") String refreshToken) {
        String accessToken = TokenUtils.parseBearerToken(authorization);
        authService.logout(accessToken, refreshToken);
        return ApiResponse.ok();
    }
}
