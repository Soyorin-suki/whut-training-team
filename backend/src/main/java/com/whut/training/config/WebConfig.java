package com.whut.training.config;

import com.whut.training.interceptor.AccessTokenInterceptor;
import com.whut.training.interceptor.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AccessTokenInterceptor accessTokenInterceptor;
    private final RefreshTokenInterceptor refreshTokenInterceptor;

    public WebConfig(AccessTokenInterceptor accessTokenInterceptor,
                     RefreshTokenInterceptor refreshTokenInterceptor) {
        this.accessTokenInterceptor = accessTokenInterceptor;
        this.refreshTokenInterceptor = refreshTokenInterceptor;
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
        registry.addInterceptor(accessTokenInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/health",
                        "/api/users/register",
                        "/api/auth/login"
                );

        registry.addInterceptor(refreshTokenInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/health",
                        "/api/users/register",
                        "/api/auth/login"
                );
    }
}
