package com.whut.training.service.impl;

import com.whut.training.aspect.annotation.ServiceLog;
import com.whut.training.domain.dto.AdminCreateUserRequest;
import com.whut.training.domain.dto.UserUpdateRequest;
import com.whut.training.domain.dto.UserRegisterRequest;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.exception.BusinessException;
import com.whut.training.repository.UserRepository;
import com.whut.training.service.CodeforcesApiService;
import com.whut.training.service.UserService;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 用户服务实现。
 *
 * <p>负责普通注册、管理员创建用户、用户资料更新和用户查询。注册与改名都会调用 Codeforces 校验 handle，确保用户名与平台账号一致。当前密码仍以明文方式存储，是已知风险。
 */
@Service
@ServiceLog
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CodeforcesApiService codeforcesApiService;

    /**
     * 创建用户服务实现。
     *
     * @param userRepository      用户仓储。
     * @param codeforcesApiService Codeforces API 服务。
     */
    public UserServiceImpl(UserRepository userRepository, CodeforcesApiService codeforcesApiService) {
        this.userRepository = userRepository;
        this.codeforcesApiService = codeforcesApiService;
    }

    /**
     * 注册普通用户。
     *
     * @param request 注册请求。
     * @return 新用户。
     */
    @Override
    public User register(UserRegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(400, "username already exists");
        }

        Optional<CodeforcesApiService.CodeforcesUserProfile> profileOptional =
                codeforcesApiService.getUserInfo(request.getUsername());
        if (profileOptional.isEmpty()) {
            throw new BusinessException(400, "username is not a valid Codeforces handle");
        }

        User user = new User(
                null,
                request.getUsername(),
                normalizeNullableText(request.getEmail()),
                request.getPassword(),
                UserRole.USER,
                request.getCodeforcesRating(),
                request.getMaxRating(),
                request.getOnline(),
                request.getLastOnlineTimeSeconds(),
                normalizeNullableText(request.getAvatarUrl())
        );
        enrichFromCodeforcesIfNeeded(user, profileOptional.get());
        return userRepository.save(user);
    }

    /**
     * 管理员创建用户。
     *
     * @param request 创建请求。
     * @return 新用户。
     */
    @Override
    public User createByAdmin(AdminCreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(400, "username already exists");
        }
        User user = new User(null, request.getUsername(), request.getEmail(), request.getPassword(), request.getRole());
        return userRepository.save(user);
    }

    /**
     * 获取全部用户。
     *
     * @return 用户列表。
     */
    @Override
    public List<User> list() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getId))
                .toList();
    }

    /**
     * 按 ID 查询用户。
     *
     * @param id 用户 ID。
     * @return 用户。
     */
    @Override
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "user not found: " + id));
    }

    /**
     * 按用户名查询用户。
     *
     * @param username 用户名。
     * @return 用户。
     */
    @Override
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(404, "user not found: " + username));
    }

    /**
     * 更新用户资料。
     *
     * <p>如果用户名发生变化，会重新校验 Codeforces handle 并同步统计信息。邮箱支持置空，密码留空则不更新。
     *
     * @param userId  用户 ID。
     * @param request 更新请求。
     * @return 更新后的用户。
     */
    @Override
    public User updateProfile(Long userId, UserUpdateRequest request) {
        User user = getById(userId);
        if (request == null) {
            return user;
        }

        if (request.username() != null) {
            String nextUsername = request.username().trim();
            if (nextUsername.isEmpty()) {
                throw new BusinessException(400, "username cannot be empty");
            }
            if (!nextUsername.equals(user.getUsername())) {
                if (userRepository.existsByUsername(nextUsername)) {
                    throw new BusinessException(400, "username already exists");
                }
                Optional<CodeforcesApiService.CodeforcesUserProfile> profileOptional =
                        codeforcesApiService.getUserInfo(nextUsername);
                if (profileOptional.isEmpty()) {
                    throw new BusinessException(400, "username is not a valid Codeforces handle");
                }
                user.setUsername(nextUsername);
                syncCodeforcesStats(user, profileOptional.get());
            }
        }

        if (request.email() != null) {
            user.setEmail(normalizeNullableText(request.email()));
        }

        if (request.password() != null) {
            String password = request.password().trim();
            if (!password.isEmpty()) {
                if (password.length() < 6) {
                    throw new BusinessException(400, "password length must be at least 6");
                }
                user.setPassword(password);
            }
        }

        if (request.displayName() != null) {
            user.setDisplayName(normalizeNullableText(request.displayName()));
        }

        if (request.avatar() != null) {
            user.setAvatarUrl(normalizeNullableText(request.avatar()));
        }

        if (request.bio() != null) {
            user.setBio(normalizeNullableText(request.bio()));
        }

        return userRepository.save(user);
    }

    /**
     * 将 Codeforces 统计信息写回用户实体。
     *
     * @param user    用户。
     * @param profile Codeforces 资料。
     */
    private void syncCodeforcesStats(User user, CodeforcesApiService.CodeforcesUserProfile profile) {
        user.setCodeforcesRating(profile.rating());
        user.setMaxRating(profile.maxRating());
        user.setOnline(profile.online());
        user.setLastOnlineTimeSeconds(profile.lastOnlineTimeSeconds());
    }

    /**
     * 在必要时补齐 Codeforces 资料。
     *
     * @param user    用户。
     * @param profile Codeforces 资料。
     */
    private void enrichFromCodeforcesIfNeeded(User user, CodeforcesApiService.CodeforcesUserProfile profile) {
        boolean needEnrich = user.getCodeforcesRating() == null
                || user.getMaxRating() == null
                || user.getOnline() == null
                || user.getLastOnlineTimeSeconds() == null
                || user.getAvatarUrl() == null
                || user.getAvatarUrl().isBlank();
        if (!needEnrich) {
            user.setUid(parseUidFromAvatarUrl(user.getAvatarUrl()));
            return;
        }

        if (user.getCodeforcesRating() == null) {
            user.setCodeforcesRating(profile.rating());
        }
        if (user.getMaxRating() == null) {
            user.setMaxRating(profile.maxRating());
        }
        if (user.getOnline() == null) {
            user.setOnline(profile.online());
        }
        if (user.getLastOnlineTimeSeconds() == null) {
            user.setLastOnlineTimeSeconds(profile.lastOnlineTimeSeconds());
        }
        if (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(profile.avatarUrl());
        }
        user.setUid(parseUidFromAvatarUrl(user.getAvatarUrl()));
    }

    /**
     * 将空白文本规范化为 null。
     *
     * @param value 原始文本。
     * @return 规范化后的文本。
     */
    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 从头像 URL 中解析 UID。
     *
     * @param avatarUrl 头像地址。
     * @return UID；无法解析时返回 null。
     */
    private Long parseUidFromAvatarUrl(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(avatarUrl.trim());
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return null;
            }
            String[] segments = path.split("/");
            for (String segment : segments) {
                if (segment == null || segment.isBlank()) {
                    continue;
                }
                if (segment.chars().allMatch(Character::isDigit)) {
                    return Long.parseLong(segment);
                }
                break;
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }
}
