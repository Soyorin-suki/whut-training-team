package com.whut.training.service.impl;

import com.whut.training.aspect.annotation.ServiceLog;
import com.whut.training.domain.dto.LoginRequest;
import com.whut.training.domain.dto.LoginResponse;
import com.whut.training.domain.dto.RefreshTokenResponse;
import com.whut.training.domain.entity.User;
import com.whut.training.exception.BusinessException;
import com.whut.training.repository.AuthTokenSessionRepository;
import com.whut.training.repository.AuthTokenSessionRepository.AuthTokenSession;
import com.whut.training.repository.UserRepository;
import com.whut.training.service.AuthService;
import com.whut.training.service.CodeforcesApiService;
import com.whut.training.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * 认证服务实现。
 *
 * <p>负责账号密码登录、token 续期、退出登录以及登录时同步 Codeforces 头像与 UID。当前实现仍以明文密码对比为主，属于已知安全风险，后续应改为哈希存储与校验。
 */
@Service
@ServiceLog
public class AuthServiceImpl implements AuthService {

    private final long accessTokenTtlSeconds;
    private final long refreshTokenTtlSeconds;

    private final UserService userService;
    private final CodeforcesApiService codeforcesApiService;
    private final UserRepository userRepository;
    private final AuthTokenSessionRepository authTokenSessionRepository;

    /**
     * 创建认证服务实现。
     *
     * @param userService                用户服务。
     * @param codeforcesApiService       Codeforces API 服务。
     * @param userRepository             用户仓储。
     * @param authTokenSessionRepository 认证会话仓储。
     * @param accessTokenTtlSeconds      access token 有效期（秒）。
     * @param refreshTokenTtlSeconds     refresh token 有效期（秒）。
     */
    public AuthServiceImpl(
            UserService userService,
            CodeforcesApiService codeforcesApiService,
            UserRepository userRepository,
            AuthTokenSessionRepository authTokenSessionRepository,
            @Value("${app.auth.access-token-ttl-seconds:1800}") long accessTokenTtlSeconds,
            @Value("${app.auth.refresh-token-ttl-seconds:604800}") long refreshTokenTtlSeconds
    ) {
        this.userService = userService;
        this.codeforcesApiService = codeforcesApiService;
        this.userRepository = userRepository;
        this.authTokenSessionRepository = authTokenSessionRepository;
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }

    /**
     * 校验用户名和密码并签发 token。
     *
     * @param request 登录请求。
     * @return 登录响应。
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        User user;
        try {
            user = userService.getByUsername(request.getUsername());
        } catch (BusinessException ex) {
            throw new BusinessException(401, "invalid username or password");
        }
        if (!user.getPassword().equals(request.getPassword())) {
            throw new BusinessException(401, "invalid username or password");
        }
        syncAvatarFromCodeforcesOnLogin(user);

        TokenPair pair = issueTokenPair(user.getId());

        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getUid(),
                user.getCodeforcesRating(),
                user.getMaxRating(),
                user.getOnline(),
                user.getLastOnlineTimeSeconds(),
                user.getAvatarUrl(),
                pair.accessToken(),
                pair.refreshToken()
        );
    }

    /**
     * 使用 refresh token 换取新的 token 对。
     *
     * @param refreshToken 刷新令牌。
     * @return 新的刷新响应。
     */
    @Override
    public RefreshTokenResponse refresh(String refreshToken) {
        AuthTokenSession refreshSession = validateRefreshSession(refreshToken);
        authTokenSessionRepository.deleteByRefreshToken(refreshToken);
        authTokenSessionRepository.deleteByAccessToken(refreshSession.accessToken());

        TokenPair nextPair = issueTokenPair(refreshSession.userId());
        return new RefreshTokenResponse(nextPair.accessToken(), nextPair.refreshToken());
    }

    /**
     * 注销登录会话。
     *
     * @param accessToken  可选的 access token。
     * @param refreshToken refresh token。
     */
    @Override
    public void logout(String accessToken, String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(401, "invalid refresh token");
        }

        AuthTokenSession refreshSession = authTokenSessionRepository.findByRefreshToken(refreshToken).orElse(null);
        if (refreshSession == null) {
            throw new BusinessException(401, "invalid refresh token");
        }

        authTokenSessionRepository.deleteByRefreshToken(refreshToken);
        authTokenSessionRepository.deleteByAccessToken(refreshSession.accessToken());
        if (accessToken != null && !accessToken.isBlank()) {
            authTokenSessionRepository.deleteByAccessToken(accessToken);
        }
    }

    /**
     * 校验 access token 并读取用户。
     *
     * @param accessToken access token。
     * @return 当前用户。
     */
    @Override
    public User validateAccessTokenAndGetUser(String accessToken) {
        AuthTokenSession accessSession = validateAccessSession(accessToken);
        return userService.getById(accessSession.userId());
    }

    /**
     * 校验 access token 是否有效。
     *
     * @param accessToken access token。
     * @return 对应会话。
     */
    private AuthTokenSession validateAccessSession(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException(401, "invalid access token");
        }
        AuthTokenSession accessSession = authTokenSessionRepository.findByAccessToken(accessToken)
                .orElseThrow(() -> new BusinessException(401, "invalid access token"));
        Instant now = Instant.now();
        long nowSeconds = now.getEpochSecond();
        if (nowSeconds >= accessSession.accessExpiredAtSeconds()) {
            authTokenSessionRepository.deleteByAccessToken(accessToken);
            throw new BusinessException(401, "access token expired");
        }
        return accessSession;
    }

    /**
     * 校验 refresh token 是否有效。
     *
     * @param refreshToken refresh token。
     * @return 对应会话。
     */
    private AuthTokenSession validateRefreshSession(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(401, "invalid refresh token");
        }

        AuthTokenSession refreshSession = authTokenSessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new BusinessException(401, "invalid refresh token"));

        Instant now = Instant.now();
        long nowSeconds = now.getEpochSecond();
        if (nowSeconds >= refreshSession.refreshExpiredAtSeconds()) {
            authTokenSessionRepository.deleteByRefreshToken(refreshToken);
            authTokenSessionRepository.deleteByAccessToken(refreshSession.accessToken());
            throw new BusinessException(401, "refresh token expired");
        }
        return refreshSession;
    }

    /**
     * 签发新的 token 对并持久化会话。
     *
     * @param userId 用户 ID。
     * @return token 对。
     */
    private TokenPair issueTokenPair(Long userId) {
        authTokenSessionRepository.deleteExpiredBefore(Instant.now().getEpochSecond());

        String accessToken = UUID.randomUUID().toString();
        String refreshToken = UUID.randomUUID().toString();
        Instant now = Instant.now();
        AuthTokenSession session = new AuthTokenSession(
                userId,
                accessToken,
                refreshToken,
                now.plusSeconds(accessTokenTtlSeconds).getEpochSecond(),
                now.plusSeconds(refreshTokenTtlSeconds).getEpochSecond()
        );
        try {
            authTokenSessionRepository.save(session);
        } catch (DataAccessException ex) {
            throw new BusinessException(500, "failed to issue token");
        }
        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * 登录时同步用户头像与 UID。
     *
     * @param user 当前用户。
     */
    private void syncAvatarFromCodeforcesOnLogin(User user) {
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
            return;
        }
        codeforcesApiService.getUserInfo(user.getUsername()).ifPresent(profile -> {
            String latestAvatarUrl = profile.avatarUrl();
            Long latestUid = parseUidFromAvatarUrl(latestAvatarUrl);
            boolean changed = false;

            if (latestAvatarUrl != null && !latestAvatarUrl.isBlank() && !latestAvatarUrl.equals(user.getAvatarUrl())) {
                user.setAvatarUrl(latestAvatarUrl);
                changed = true;
            }
            if (latestUid != null && !latestUid.equals(user.getUid())) {
                user.setUid(latestUid);
                changed = true;
            }
            if (changed) {
                userRepository.save(user);
            }
        });
    }

    /**
     * 从头像 URL 中解析 UID。
     *
     * @param avatarUrl Codeforces 头像地址。
     * @return UID；若无法解析则返回 null。
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

    /**
     * 一对 token 的简单封装。
     *
     * @param accessToken  access token。
     * @param refreshToken refresh token。
     */
    private record TokenPair(String accessToken, String refreshToken) {
    }
}
