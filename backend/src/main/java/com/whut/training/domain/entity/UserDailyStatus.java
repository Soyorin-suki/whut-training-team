package com.whut.training.domain.entity;

import java.time.LocalDate;

/**
 * 用户每日题打卡记录。
 *
 * @param userId       用户 ID。
 * @param date         日期。
 * @param submissionId 提交 ID。
 * @param verdict      判题结果。
 * @param score        得分。
 */
public record UserDailyStatus(
        Long userId,
        LocalDate date,
        Long submissionId,
        String verdict,
        Integer score
) {
}
