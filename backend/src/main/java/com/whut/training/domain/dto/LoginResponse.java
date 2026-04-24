package com.whut.training.domain.dto;

import com.whut.training.domain.enums.UserRole;

public record LoginResponse(Long userId, String username, String email, UserRole role, String accessToken,
                            String refreshToken) {
}
