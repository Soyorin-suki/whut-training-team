package com.whut.training.service.impl;

import com.whut.training.domain.dto.LoginRequest;
import com.whut.training.domain.dto.LoginResponse;
import com.whut.training.domain.entity.User;
import com.whut.training.exception.BusinessException;
import com.whut.training.service.AuthService;
import com.whut.training.service.UserService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthServiceImpl implements AuthService {

    private static final long ACCESS_TOKEN_TTL_SECONDS = 30 * 60;
    private static final long REFRESH_TOKEN_TTL_SECONDS = 7 * 24 * 60 * 60;

    private final UserService userService;
    private final ConcurrentHashMap<String, Session> accessStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> refreshToAccessStore = new ConcurrentHashMap<>();

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

        String accessToken = UUID.randomUUID().toString();
        String refreshToken = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Session session = new Session(
                user.getId(),
                refreshToken,
                now.plusSeconds(ACCESS_TOKEN_TTL_SECONDS),
                now.plusSeconds(REFRESH_TOKEN_TTL_SECONDS)
        );
        accessStore.put(accessToken, session);
        refreshToAccessStore.put(refreshToken, accessToken);

        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                accessToken,
                refreshToken
        );
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        Session session = validateSession(accessToken, refreshToken);
        accessStore.remove(accessToken);
        refreshToAccessStore.remove(session.refreshToken());
    }

    @Override
    public User validateAndGetUser(String accessToken, String refreshToken) {
        Session session = validateSession(accessToken, refreshToken);
        return userService.getById(session.userId());
    }

    private Session validateSession(String accessToken, String refreshToken) {
        Session session = accessStore.get(accessToken);
        if (session == null) {
            throw new BusinessException(401, "invalid token pair");
        }

        if (!session.refreshToken().equals(refreshToken)) {
            throw new BusinessException(401, "invalid token pair");
        }

        String linkedAccessToken = refreshToAccessStore.get(refreshToken);
        if (linkedAccessToken == null || !linkedAccessToken.equals(accessToken)) {
            throw new BusinessException(401, "invalid token pair");
        }

        Instant now = Instant.now();
        if (now.isAfter(session.accessExpiredAt()) || now.isAfter(session.refreshExpiredAt())) {
            accessStore.remove(accessToken);
            refreshToAccessStore.remove(refreshToken);
            throw new BusinessException(401, "token expired");
        }
        return session;
    }

    private record Session(Long userId, String refreshToken, Instant accessExpiredAt, Instant refreshExpiredAt) {
    }
}
