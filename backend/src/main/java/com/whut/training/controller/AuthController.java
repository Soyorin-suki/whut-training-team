package com.whut.training.controller;

import com.whut.training.common.ApiResponse;
import com.whut.training.domain.dto.LoginRequest;
import com.whut.training.domain.dto.LoginResponse;
import com.whut.training.domain.dto.RefreshTokenResponse;
import com.whut.training.service.AuthService;
import com.whut.training.utils.TokenUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口。
 *
 * <p>负责登录、刷新令牌和退出登录。前端在 token 过期时依赖刷新接口自动恢复会话。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * 创建认证控制器。
     *
     * @param authService 认证服务。
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户登录。
     *
     * @param request 登录请求体。
     * @return 登录响应，包含用户信息和双 token。
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    /**
     * 刷新访问令牌。
     *
     * @param refreshToken 刷新令牌请求头。
     * @return 新的访问令牌与刷新令牌。
     */
    @PostMapping("/refresh")
    public ApiResponse<RefreshTokenResponse> refresh(@RequestHeader("X-Refresh-Token") String refreshToken) {
        return ApiResponse.ok(authService.refresh(refreshToken));
    }

    /**
     * 退出登录。
     *
     * @param authorization 可选的 Authorization 头，用于同时注销 access token。
     * @param refreshToken   刷新令牌请求头。
     * @return 空成功响应。
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @RequestHeader("X-Refresh-Token") String refreshToken) {
        String accessToken = authorization == null ? null : TokenUtils.parseBearerToken(authorization);
        authService.logout(accessToken, refreshToken);
        return ApiResponse.ok();
    }
}
