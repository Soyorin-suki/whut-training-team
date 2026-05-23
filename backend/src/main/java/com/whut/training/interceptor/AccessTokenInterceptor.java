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

/**
 * 访问令牌拦截器。
 *
 * <p>拦截需要登录的 API，请求头中读取 Bearer access token 并写入当前用户上下文。若 token 无效则直接返回 401。
 */
@Component
public class AccessTokenInterceptor implements HandlerInterceptor {

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    /**
     * 创建访问令牌拦截器。
     *
     * @param authService 认证服务。
     * @param objectMapper JSON 序列化器。
     */
    public AccessTokenInterceptor(AuthService authService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    /**
     * 请求前校验访问令牌。
     *
     * @param request  HTTP 请求。
     * @param response HTTP 响应。
     * @param handler  当前处理器。
     * @return 校验通过返回 true，否则返回 false。
     */
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

    /**
     * 请求完成后清理用户上下文。
     *
     * @param request  HTTP 请求。
     * @param response HTTP 响应。
     * @param handler  当前处理器。
     * @param ex       请求处理期间抛出的异常。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
