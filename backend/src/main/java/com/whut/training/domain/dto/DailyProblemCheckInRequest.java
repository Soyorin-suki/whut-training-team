package com.whut.training.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 每日题打卡请求。
 *
 * @param submissionId Codeforces 提交 ID。
 */
public record DailyProblemCheckInRequest(
        @NotNull(message = "submissionId is required")
        @Positive(message = "submissionId must be > 0")
        Long submissionId
) {
}
