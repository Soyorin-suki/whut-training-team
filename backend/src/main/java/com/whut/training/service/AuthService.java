package com.whut.training.service;

import com.whut.training.domain.dto.LoginRequest;
import com.whut.training.domain.dto.LoginResponse;
import com.whut.training.domain.dto.RefreshTokenResponse;
import com.whut.training.domain.entity.User;

/**
 * 认证服务。
 *
 * <p>负责登录、刷新、退出和访问令牌校验，同时与 Codeforces 用户信息同步联动。
 */
public interface AuthService {
    /**
     * 用户登录并签发双 token。
     *
     * @param request 登录请求。
     * @return 登录响应。
     */
    LoginResponse login(LoginRequest request);

    /**
     * 使用刷新令牌换取新 token。
     *
     * @param refreshToken 刷新令牌。
     * @return 新的 access token 和 refresh token。
     */
    RefreshTokenResponse refresh(String refreshToken);

    /**
     * 退出登录并回收 token。
     *
     * @param accessToken  访问令牌，可为空。
     * @param refreshToken 刷新令牌。
     */
    void logout(String accessToken, String refreshToken);

    /**
     * 校验 access token 并读取用户。
     *
     * @param accessToken 访问令牌。
     * @return 当前用户。
     */
    User validateAccessTokenAndGetUser(String accessToken);
}
