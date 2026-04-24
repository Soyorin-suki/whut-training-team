package com.whut.training.domain.dto;

import com.whut.training.domain.enums.UserRole;

public record LoginResponse(Long id, String username, String email, UserRole role, Long uid, Integer codeforcesRating,
                            Integer maxRating, Boolean online, Long lastOnlineTimeSeconds, String avatarUrl,
                            String accessToken, String refreshToken) {
}
