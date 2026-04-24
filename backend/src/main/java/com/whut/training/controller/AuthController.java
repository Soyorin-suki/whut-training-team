package com.whut.training.controller;

import com.whut.training.common.ApiResponse;
import com.whut.training.domain.dto.LoginRequest;
import com.whut.training.domain.dto.LoginResponse;
import com.whut.training.domain.dto.RefreshTokenResponse;
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

    @PostMapping("/refresh")
    public ApiResponse<RefreshTokenResponse> refresh(@RequestHeader("X-Refresh-Token") String refreshToken) {
        return ApiResponse.ok(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @RequestHeader("X-Refresh-Token") String refreshToken) {
        String accessToken = authorization == null ? null : TokenUtils.parseBearerToken(authorization);
        authService.logout(accessToken, refreshToken);
        return ApiResponse.ok();
    }
}
