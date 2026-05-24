package com.whut.training.domain.dto;

/**
 * 刷新令牌响应。
 *
 * @param accessToken  新的访问令牌。
 * @param refreshToken 新的刷新令牌。
 */
public record RefreshTokenResponse(String accessToken, String refreshToken) {
}
