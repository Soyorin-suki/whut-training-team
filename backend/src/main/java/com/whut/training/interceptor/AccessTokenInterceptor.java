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
import org.springframework.util.AntPathMatcher;

@Component
public class AccessTokenInterceptor implements HandlerInterceptor {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

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
            if ((authorization == null || authorization.isBlank()) && isOptionalPublicReadRequest(request)) {
                UserContext.clear();
                return true;
            }
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

    private boolean isOptionalPublicReadRequest(HttpServletRequest request) {
        if (!RequestMethod.GET.name().equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        return PATH_MATCHER.match("/api/problems/*", uri)
                || PATH_MATCHER.match("/api/problem-comments/*", uri);
    }
}
