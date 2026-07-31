package com.whut.training.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 开始 Codeforces 账号绑定请求。
 */
public class CodeforcesBindingStartRequest {

    @NotBlank(message = "Codeforces handle cannot be blank")
    @Size(max = 64, message = "Codeforces handle length must be <= 64")
    private String handle;

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }
}
