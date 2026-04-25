package com.whut.training.interceptor;

import com.whut.training.context.UserContext;
import com.whut.training.domain.entity.User;
import com.whut.training.exception.BusinessException;
import com.whut.training.service.AuthService;
import com.whut.training.utils.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AccessTokenInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    public AccessTokenInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (RequestMethod.OPTIONS.name().equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        try {
            String authorization = request.getHeader("Authorization");
            String accessToken = TokenUtils.parseBearerToken(authorization);
            User user = authService.validateAccessTokenAndGetUser(accessToken);
            UserContext.setCurrentUser(user);
            return true;
        } catch (BusinessException ex) {
            if (ex.getCode() == 401) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
            throw ex;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
