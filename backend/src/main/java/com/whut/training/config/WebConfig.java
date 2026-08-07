package com.whut.training.config;

import com.whut.training.interceptor.AccessTokenInterceptor;
import com.whut.training.interceptor.RequestLoggingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置。
 *
 * <p>负责注册跨域、请求日志与访问令牌拦截器。当前前端仅允许来自本地开发端口的跨域访问。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AccessTokenInterceptor accessTokenInterceptor;
    private final RequestLoggingInterceptor requestLoggingInterceptor;

        /**
         * 创建 Web MVC 配置。
         *
         * @param accessTokenInterceptor   访问令牌拦截器。
         * @param requestLoggingInterceptor 请求日志拦截器。
         */
    public WebConfig(AccessTokenInterceptor accessTokenInterceptor,
                     RequestLoggingInterceptor requestLoggingInterceptor) {
        this.accessTokenInterceptor = accessTokenInterceptor;
        this.requestLoggingInterceptor = requestLoggingInterceptor;
    }

        /**
         * 配置跨域规则。
         *
         * @param registry CORS 注册器。
         */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("*")
                .allowedHeaders("*")
                .exposedHeaders("Content-Disposition")
                .allowCredentials(true);
    }

        /**
         * 注册拦截器。
         *
         * @param registry 拦截器注册器。
         */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestLoggingInterceptor)
                .addPathPatterns("/api/**");

        registry.addInterceptor(accessTokenInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/health",
                        "/api/users/register",
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/auth/logout"
                );
    }

}
