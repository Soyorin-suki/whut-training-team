package com.whut.training.service;

import com.whut.training.domain.dto.LoginRequest;
import com.whut.training.domain.dto.LoginResponse;
import com.whut.training.domain.entity.User;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    void logout(String accessToken, String refreshToken);

    User validateAndGetUser(String accessToken, String refreshToken);
}
