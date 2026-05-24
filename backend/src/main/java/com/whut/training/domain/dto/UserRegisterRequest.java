package com.whut.training.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求。
 *
 * <p>注册时会校验 username 是否为合法 Codeforces handle。当前还暴露了可选的 Codeforces 资料字段，主要用于后续补录或测试场景。
 *
 * @param username             用户名 / Codeforces handle。
 * @param email                邮箱。
 * @param password             密码。
 * @param codeforcesRating     当前 rating。
 * @param maxRating            历史最高 rating。
 * @param online               在线状态。
 * @param lastOnlineTimeSeconds 最近在线时间戳。
 * @param avatarUrl            头像地址。
 */
public class UserRegisterRequest {

    @NotBlank(message = "username cannot be blank")
    @Size(max = 50, message = "username length must be <= 50")
    private String username;

    @Email(message = "email format is invalid")
    private String email;

    @NotBlank(message = "password cannot be blank")
    @Size(min = 6, max = 64, message = "password length must be 6-64")
    private String password;

    private Integer codeforcesRating;
    private Integer maxRating;
    private Boolean online;
    private Long lastOnlineTimeSeconds;
    private String avatarUrl;

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
     * 获取当前 rating。
     *
     * @return 当前 rating。
     */
    public Integer getCodeforcesRating() {
        return codeforcesRating;
    }

    /**
     * 设置当前 rating。
     *
     * @param codeforcesRating 当前 rating。
     */
    public void setCodeforcesRating(Integer codeforcesRating) {
        this.codeforcesRating = codeforcesRating;
    }

    /**
     * 获取历史最高 rating。
     *
     * @return 历史最高 rating。
     */
    public Integer getMaxRating() {
        return maxRating;
    }

    /**
     * 设置历史最高 rating。
     *
     * @param maxRating 历史最高 rating。
     */
    public void setMaxRating(Integer maxRating) {
        this.maxRating = maxRating;
    }

    /**
     * 获取在线状态。
     *
     * @return 在线状态。
     */
    public Boolean getOnline() {
        return online;
    }

    /**
     * 设置在线状态。
     *
     * @param online 在线状态。
     */
    public void setOnline(Boolean online) {
        this.online = online;
    }

    /**
     * 获取最近在线时间戳。
     *
     * @return 最近在线时间戳。
     */
    public Long getLastOnlineTimeSeconds() {
        return lastOnlineTimeSeconds;
    }

    /**
     * 设置最近在线时间戳。
     *
     * @param lastOnlineTimeSeconds 最近在线时间戳。
     */
    public void setLastOnlineTimeSeconds(Long lastOnlineTimeSeconds) {
        this.lastOnlineTimeSeconds = lastOnlineTimeSeconds;
    }

    /**
     * 获取头像地址。
     *
     * @return 头像地址。
     */
    public String getAvatarUrl() {
        return avatarUrl;
    }

    /**
     * 设置头像地址。
     *
     * @param avatarUrl 头像地址。
     */
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
