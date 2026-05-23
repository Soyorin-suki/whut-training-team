package com.whut.training.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 请求日志拦截器。
 *
 * <p>记录每个 API 的方法、路径、状态码、耗时和异常类型，用于排查接口问题。日志本身不修改业务响应。
 */
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String START_TIME_ATTR = RequestLoggingInterceptor.class.getName() + ".START_TIME";

    /**
     * 在请求开始时记录起始时间。
     *
     * @param request  HTTP 请求。
     * @param response HTTP 响应。
     * @param handler  当前处理器。
     * @return 始终返回 true。
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.nanoTime());
        return true;
    }

    /**
     * 在请求结束后输出访问日志。
     *
     * @param request  HTTP 请求。
     * @param response HTTP 响应。
     * @param handler  当前处理器。
     * @param ex       请求处理期间抛出的异常。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        long durationMs = -1L;
        Object start = request.getAttribute(START_TIME_ATTR);
        if (start instanceof Long startNs) {
            durationMs = (System.nanoTime() - startNs) / 1_000_000;
        }

        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String path = query == null || query.isBlank() ? uri : uri + "?" + query;
        String clientIp = request.getRemoteAddr();

        if (ex == null) {
            log.info("request method={} path={} status={} ip={} durationMs={}",
                    request.getMethod(), path, response.getStatus(), clientIp, durationMs);
            return;
        }

        log.warn("request method={} path={} status={} ip={} durationMs={} exception={}",
                request.getMethod(), path, response.getStatus(), clientIp, durationMs, ex.getClass().getSimpleName());
    }
}
