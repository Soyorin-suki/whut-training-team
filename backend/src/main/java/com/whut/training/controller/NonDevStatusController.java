package com.whut.training.controller;

import com.whut.training.common.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 非开发环境的调试能力探测接口。
 *
 * <p>前端开发服务器会调用该接口判断是否显示调试面板。生产及默认 profile
 * 明确返回未启用，避免把一个正常的能力探测记录成 404/500。
 */
@RestController
@RequestMapping("/api/dev")
@Profile("!dev")
public class NonDevStatusController {

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.ok(Map.of(
                "active", false,
                "profile", "non-dev"
        ));
    }
}
