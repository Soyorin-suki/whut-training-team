package com.whut.training.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 用户资料更新请求。
 *
 * @param username    站内用户名。
 * @param email       邮箱，可置空。
 * @param password    新密码，留空表示不修改。
 * @param displayName 展示昵称。
 * @param avatar      头像地址。
 * @param bio         个人简介。
 * @param showProblemTags 每日一题是否显示标签。
 */
public record UserUpdateRequest(
        @Size(min = 1, max = 64, message = "username length must be 1-64")
        String username,
        @Email(message = "email format is invalid")
        @Size(max = 255, message = "email is too long")
        String email,
        @Size(min = 8, max = 64, message = "password length must be 8-64")
        String password,
        @Size(max = 100, message = "display name is too long")
        String displayName,
        @Size(max = 700000, message = "avatar is too large")
        String avatar,
        @Size(max = 1000, message = "bio is too long")
        String bio,
        Boolean showProblemTags
) {
}
