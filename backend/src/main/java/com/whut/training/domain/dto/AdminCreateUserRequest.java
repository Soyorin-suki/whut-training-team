package com.whut.training.domain.dto;

import com.whut.training.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 管理员创建用户请求。
 *
 * @param username 用户名。
 * @param email    邮箱。
 * @param password 密码。
 * @param role     角色。
 */
public class AdminCreateUserRequest {

    @NotBlank(message = "username cannot be blank")
    @Size(max = 50, message = "username length must be <= 50")
    private String username;

    @NotBlank(message = "email cannot be blank")
    @Email(message = "email format is invalid")
    @Size(max = 255, message = "email length must be <= 255")
    private String email;

    @NotBlank(message = "password cannot be blank")
    @Size(min = 8, max = 64, message = "password length must be 8-64")
    private String password;

    @NotNull(message = "role cannot be null")
    private UserRole role;

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
     * 获取邮箱。
     *
     * @return 邮箱。
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置邮箱。
     *
     * @param email 邮箱。
     */
    public void setEmail(String email) {
        this.email = email;
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

    /**
     * 获取用户角色。
     *
     * @return 用户角色。
     */
    public UserRole getRole() {
        return role;
    }

    /**
     * 设置用户角色。
     *
     * @param role 用户角色。
     */
    public void setRole(UserRole role) {
        this.role = role;
    }
}
