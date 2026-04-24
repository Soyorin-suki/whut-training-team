package com.whut.training.interceptor;

import com.whut.training.utils.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AccessTokenInterceptor implements HandlerInterceptor {

    public static final String ACCESS_TOKEN_ATTR = "accessToken";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader("Authorization");
        request.setAttribute(ACCESS_TOKEN_ATTR, TokenUtils.parseBearerToken(authorization));
        return true;
    }
}
