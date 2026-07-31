package com.whut.training.domain.dto;

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
        String username,
        String email,
        String password,
        String displayName,
        String avatar,
        String bio,
        Boolean showProblemTags
) {
}
