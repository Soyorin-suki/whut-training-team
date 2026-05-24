package com.whut.training.context;

import com.whut.training.domain.entity.User;

/**
 * 基于 ThreadLocal 的当前登录用户上下文。
 *
 * <p>拦截器会在请求进入时写入当前用户，请求完成后清理。该上下文仅适用于一次 HTTP 请求线程内的临时共享。
 */
public final class UserContext {

    private static final ThreadLocal<User> CURRENT_USER = new ThreadLocal<>();

    /**
     * 工具类禁止实例化。
     */
    private UserContext() {
    }

    /**
     * 写入当前请求对应的登录用户。
     *
     * @param user 当前登录用户，允许为空时由调用方决定。
     */
    public static void setCurrentUser(User user) {
        CURRENT_USER.set(user);
    }

    /**
     * 读取当前请求对应的登录用户。
     *
     * @return 当前登录用户；如果未注入则返回 null。
     */
    public static User getCurrentUser() {
        return CURRENT_USER.get();
    }

    /**
     * 清理当前线程中的用户上下文。
     */
    public static void clear() {
        CURRENT_USER.remove();
    }
}
