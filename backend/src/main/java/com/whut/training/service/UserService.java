package com.whut.training.service;

import com.whut.training.domain.dto.AdminCreateUserRequest;
import com.whut.training.domain.dto.UserUpdateRequest;
import com.whut.training.domain.dto.UserRegisterRequest;
import com.whut.training.domain.entity.User;

import java.util.List;

/**
 * 用户服务。
 *
 * <p>负责注册、管理员创建用户、资料更新以及用户查询。注册和更新时会与 Codeforces 账号信息联动。
 */
public interface UserService {
    /**
     * 注册普通用户。
     *
     * @param request 注册请求。
     * @return 新用户。
     */
    User register(UserRegisterRequest request);

    /**
     * 管理员创建用户。
     *
     * @param request 创建请求。
     * @return 新用户。
     */
    User createByAdmin(AdminCreateUserRequest request);

    /**
     * 获取全部用户。
     *
     * @return 用户列表。
     */
    List<User> list();

    /**
     * 按 ID 查询用户。
     *
     * @param id 用户 ID。
     * @return 用户。
     */
    User getById(Long id);

    /**
     * 按用户名查询用户。
     *
     * @param username 用户名 / Codeforces handle。
     * @return 用户。
     */
    User getByUsername(String username);

    /**
     * 更新指定用户资料。
     *
     * @param userId  用户 ID。
     * @param request 更新请求。
     * @return 更新后的用户。
     */
    User updateProfile(Long userId, UserUpdateRequest request);
}
