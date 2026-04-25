package com.whut.training.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whut.training.common.ApiResponse;
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
    private final ObjectMapper objectMapper;

    public AccessTokenInterceptor(AuthService authService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.objectMapper = objectMapper;
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
                response.setContentType("application/json;charset=UTF-8");
                try {
                    response.getWriter().write(
                            objectMapper.writeValueAsString(ApiResponse.fail(401, ex.getMessage()))
                    );
                } catch (Exception ignore) {
                    // best effort
                }
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
