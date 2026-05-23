package com.whut.training.domain.dto;

/**
 * 打卡或练习题校验结果。
 *
 * @param type         类型，通常为 DAILY 或 PRACTICE。
 * @param accepted     是否通过。
 * @param submissionId 提交 ID。
 * @param verdict      判题结果。
 * @param score        得分。
 */
public record CheckInResultResponse(
        String type,
        boolean accepted,
        Long submissionId,
        String verdict,
        Integer score
) {
}
