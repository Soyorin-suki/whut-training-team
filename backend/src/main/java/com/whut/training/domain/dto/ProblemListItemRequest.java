package com.whut.training.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 向题单添加或修改一道题目。 */
public record ProblemListItemRequest(
        @Size(max = 255, message = "题目标题不能超过 255 个字符")
        String title,
        @NotBlank(message = "题目链接不能为空")
        @Size(max = 1000, message = "题目链接不能超过 1000 个字符")
        String link,
        @Size(max = 1000, message = "备注不能超过 1000 个字符")
        String note,
        @Size(max = 64, message = "题目编号不能超过 64 个字符")
        String problemKey,
        Integer rating,
        @Size(max = 1000, message = "标签不能超过 1000 个字符")
        String tags
) {
}
