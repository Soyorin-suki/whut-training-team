package com.whut.training.controller;

import com.whut.training.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 健康检查接口。
 *
 * <p>用于确认服务存活与当前时间，通常给前端或运维探活使用。
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    /**
     * 获取健康状态。
     *
     * @return 包含状态与当前时间的统一响应。
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(Map.of(
                "status", "UP",
                "time", LocalDateTime.now().toString()
        ));
    }
}

