package com.whut.training.domain.dto;

/**
 * 自主练习历史条目。
 *
 * @param drawId        抽题记录 ID。
 * @param drawDate      抽题日期。
 * @param problemKey    题目唯一键。
 * @param contestId     比赛 ID。
 * @param problemIndex  题号。
 * @param name          题目标题。
 * @param rating        难度。
 * @param tags          标签。
 * @param sourceUrl     题目链接。
 * @param drawnAt       抽题时间。
 * @param submissionId  提交 ID。
 * @param verdict       判题结果。
 */
public record PracticeHistoryItem(
        Long drawId,
        String drawDate,
        String problemKey,
        Integer contestId,
        String problemIndex,
        String name,
        Integer rating,
        String tags,
        String sourceUrl,
        String drawnAt,
        Long submissionId,
        String verdict
) {
}
