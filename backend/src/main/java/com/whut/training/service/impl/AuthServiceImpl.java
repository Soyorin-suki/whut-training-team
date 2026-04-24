package com.whut.training.service.impl;

import com.whut.training.aspect.annotation.ServiceLog;
import com.whut.training.domain.dto.LoginRequest;
import com.whut.training.domain.dto.LoginResponse;
import com.whut.training.domain.dto.RefreshTokenResponse;
import com.whut.training.domain.entity.User;
import com.whut.training.exception.BusinessException;
import com.whut.training.service.AuthService;
import com.whut.training.service.UserService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ServiceLog
public class AuthServiceImpl implements AuthService {

    private static final long ACCESS_TOKEN_TTL_SECONDS = 30 * 60;
    private static final long REFRESH_TOKEN_TTL_SECONDS = 7 * 24 * 60 * 60;

    private final UserService userService;
    private final ConcurrentHashMap<String, AccessSession> accessStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RefreshSession> refreshStore = new ConcurrentHashMap<>();

    public AuthServiceImpl(UserService userService) {
        this.userService = userService;
    }

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

    @Override
    public RefreshTokenResponse refresh(String refreshToken) {
        RefreshSession refreshSession = validateRefreshSession(refreshToken);
        accessStore.remove(refreshSession.accessToken());
        refreshStore.remove(refreshToken);

        TokenPair nextPair = issueTokenPair(refreshSession.userId());
        return new RefreshTokenResponse(nextPair.accessToken(), nextPair.refreshToken());
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(401, "invalid refresh token");
        }

        RefreshSession refreshSession = refreshStore.remove(refreshToken);
        if (refreshSession == null) {
            throw new BusinessException(401, "invalid refresh token");
        }

        accessStore.remove(refreshSession.accessToken());
        if (accessToken != null && !accessToken.isBlank()) {
            accessStore.remove(accessToken);
        }
    }

    @Override
    public User validateAccessTokenAndGetUser(String accessToken) {
        AccessSession accessSession = validateAccessSession(accessToken);
        return userService.getById(accessSession.userId());
    }

    private AccessSession validateAccessSession(String accessToken) {
        AccessSession accessSession = accessStore.get(accessToken);
        if (accessSession == null) {
            throw new BusinessException(401, "invalid access token");
        }
        Instant now = Instant.now();
        if (now.isAfter(accessSession.accessExpiredAt())) {
            accessStore.remove(accessToken);
            throw new BusinessException(401, "access token expired");
        }
        return accessSession;
    }

    private RefreshSession validateRefreshSession(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(401, "invalid refresh token");
        }

        RefreshSession refreshSession = refreshStore.get(refreshToken);
        if (refreshSession == null) {
            throw new BusinessException(401, "invalid refresh token");
        }

        Instant now = Instant.now();
        if (now.isAfter(refreshSession.refreshExpiredAt())) {
            refreshStore.remove(refreshToken);
            accessStore.remove(refreshSession.accessToken());
            throw new BusinessException(401, "refresh token expired");
        }
        return refreshSession;
    }

    private TokenPair issueTokenPair(Long userId) {
        String accessToken = UUID.randomUUID().toString();
        String refreshToken = UUID.randomUUID().toString();
        Instant now = Instant.now();
        AccessSession accessSession = new AccessSession(userId, now.plusSeconds(ACCESS_TOKEN_TTL_SECONDS));
        RefreshSession refreshSession = new RefreshSession(
                userId,
                accessToken,
                now.plusSeconds(REFRESH_TOKEN_TTL_SECONDS)
        );
        accessStore.put(accessToken, accessSession);
        refreshStore.put(refreshToken, refreshSession);
        return new TokenPair(accessToken, refreshToken);
    }

    private record AccessSession(Long userId, Instant accessExpiredAt) {
    }

    private record RefreshSession(Long userId, String accessToken, Instant refreshExpiredAt) {
    }

    private record TokenPair(String accessToken, String refreshToken) {
    }
}
