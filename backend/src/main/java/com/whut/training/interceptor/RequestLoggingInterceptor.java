package com.whut.training.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String START_TIME_ATTR = RequestLoggingInterceptor.class.getName() + ".START_TIME";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.nanoTime());
        return true;
    }

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
