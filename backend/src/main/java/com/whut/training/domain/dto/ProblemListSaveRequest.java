package com.whut.training.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 创建或修改题单。 */
public record ProblemListSaveRequest(
        @NotBlank(message = "题单名称不能为空")
        @Size(max = 80, message = "题单名称不能超过 80 个字符")
        String name,
        @Size(max = 500, message = "题单简介不能超过 500 个字符")
        String description,
        Boolean shared
) {
}
