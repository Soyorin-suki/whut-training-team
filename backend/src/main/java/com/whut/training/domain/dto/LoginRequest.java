package com.whut.training.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录请求。
 *
 * @param username 站内用户名。
 * @param password 登录密码。
 */
public class LoginRequest {

    @NotBlank(message = "username cannot be blank")
    @Size(max = 50, message = "username length must be <= 50")
    private String username;

    @NotBlank(message = "password cannot be blank")
    @Size(max = 64, message = "password length must be <= 64")
    private String password;

    /**
     * 获取用户名。
     *
     * @return 用户名。
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置用户名。
     *
     * @param username 用户名。
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取密码。
     *
     * @return 密码。
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置密码。
     *
     * @param password 密码。
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
