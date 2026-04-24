package com.whut.training.interceptor;

import com.whut.training.context.UserContext;
import com.whut.training.domain.entity.User;
import com.whut.training.exception.BusinessException;
import com.whut.training.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    public RefreshTokenInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object accessTokenObj = request.getAttribute(AccessTokenInterceptor.ACCESS_TOKEN_ATTR);
        if (!(accessTokenObj instanceof String accessToken) || accessToken.isBlank()) {
            throw new BusinessException(401, "invalid token pair");
        }

        String refreshToken = request.getHeader("X-Refresh-Token");
        User user = authService.validateAndGetUser(accessToken, refreshToken);
        UserContext.setCurrentUser(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
