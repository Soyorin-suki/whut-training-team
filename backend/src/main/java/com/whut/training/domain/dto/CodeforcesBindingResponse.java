package com.whut.training.domain.dto;

/**
 * Codeforces 绑定验证信息。
 *
 * @param handle              待绑定的 Codeforces Handle。
 * @param expiresAtSeconds    验证过期时间（Unix 秒）。
 * @param verificationUrl     用于验证所有权的题目地址。
 * @param message             操作提示。
 */
public record CodeforcesBindingResponse(
        String handle,
        long expiresAtSeconds,
        String verificationUrl,
        String message
) {
}
