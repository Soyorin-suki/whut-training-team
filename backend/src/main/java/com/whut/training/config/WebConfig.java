package com.whut.training.config;

import com.whut.training.interceptor.AccessTokenInterceptor;
import com.whut.training.interceptor.RequestLoggingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AccessTokenInterceptor accessTokenInterceptor;
    private final RequestLoggingInterceptor requestLoggingInterceptor;

    public WebConfig(AccessTokenInterceptor accessTokenInterceptor,
                     RequestLoggingInterceptor requestLoggingInterceptor) {
        this.accessTokenInterceptor = accessTokenInterceptor;
        this.requestLoggingInterceptor = requestLoggingInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

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
