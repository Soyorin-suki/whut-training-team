package com.whut.training.domain.dto;

import com.whut.training.domain.enums.UserRole;

/**
 * 登录响应。
 *
 * @param id                    用户 ID。
 * @param username              用户名。
 * @param email                 邮箱。
 * @param role                  用户角色。
 * @param uid                   Codeforces UID。
 * @param codeforcesRating      当前 rating。
 * @param maxRating             历史最高 rating。
 * @param online                在线状态。
 * @param lastOnlineTimeSeconds 最近在线时间戳。
 * @param avatarUrl             头像地址。
 * @param accessToken           访问令牌。
 * @param refreshToken          刷新令牌。
 */
public record LoginResponse(Long id, String username, String email, UserRole role, Long uid, Integer codeforcesRating,
                            Integer maxRating, Boolean online, Long lastOnlineTimeSeconds, String avatarUrl,
                            String accessToken, String refreshToken) {
}
