package com.whut.training.domain.entity;

import java.time.LocalDate;

/**
 * 用户自主抽题记录。
 *
 * @param id          主键。
 * @param userId      用户 ID。
 * @param drawDate    抽题日期。
 * @param problemKey  题目唯一键。
 * @param contestId   比赛 ID。
 * @param problemIndex 题号。
 * @param name        题目标题。
 * @param rating      难度。
 * @param tags        标签。
 * @param sourceUrl   题目链接。
 * @param submissionId 校验时提交 ID。
 * @param verdict     校验结果。
 */
public record UserPracticeDraw(
        Long id,
        Long userId,
        LocalDate drawDate,
        String problemKey,
        Integer contestId,
        String problemIndex,
        String name,
        Integer rating,
        String tags,
        String sourceUrl,
        Long submissionId,
        String verdict
) {
}
