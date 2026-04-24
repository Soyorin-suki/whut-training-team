package com.whut.training.service;

import com.whut.training.domain.dto.LoginRequest;
import com.whut.training.domain.dto.LoginResponse;
import com.whut.training.domain.dto.RefreshTokenResponse;
import com.whut.training.domain.entity.User;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    RefreshTokenResponse refresh(String refreshToken);

    void logout(String accessToken, String refreshToken);

    User validateAccessTokenAndGetUser(String accessToken);
}
