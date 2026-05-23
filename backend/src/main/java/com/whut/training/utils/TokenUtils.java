package com.whut.training.utils;

import com.whut.training.exception.BusinessException;

/**
 * 访问令牌工具类。
 *
 * <p>当前仅负责解析标准 Bearer Token 头。若请求头格式非法，则抛出 401 业务异常。
 */
public final class TokenUtils {

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 工具类禁止实例化。
     */
    private TokenUtils() {
    }

    /**
     * 从 Authorization 请求头中提取 Bearer token。
     *
     * @param authorization Authorization 头内容。
     * @return 解析后的 token。
     * @throws BusinessException 当请求头为空或格式非法时抛出。
     */
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
