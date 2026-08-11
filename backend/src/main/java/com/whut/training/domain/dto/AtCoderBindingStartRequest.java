package com.whut.training.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AtCoderBindingStartRequest(
        @NotBlank(message = "AtCoder Handle 不能为空")
        @Pattern(regexp = "[A-Za-z0-9_]{1,32}", message = "AtCoder Handle 格式不正确")
        String handle
) {
}
