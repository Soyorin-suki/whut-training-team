package com.whut.training.utils;

import com.whut.training.exception.BusinessException;

public final class TokenUtils {

    private static final String BEARER_PREFIX = "Bearer ";

    private TokenUtils() {
    }

    public static String parseBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(401, "invalid authorization header");
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new BusinessException(401, "invalid authorization header");
        }
        return token;
    }
}
