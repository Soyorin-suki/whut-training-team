package com.whut.training.domain.dto;

/**
 * 每日题历史条目（基于 slot）。
 *
 * @param date         日期。
 * @param slot         题位（easy/hard），兼容旧数据时可能为 null。
 * @param problemKey   题目唯一键。
 * @param name         题目标题。
 * @param rating       难度。
 * @param sourceUrl    题目链接。
 * @param isRedrawn    是否已被重抽。
 * @param checkedIn    是否已打卡。
 * @param submissionId 提交 ID。
 * @param verdict      判题结果。
 * @param score        得分。
 */
public record DailyProblemHistoryItem(
        String date,
        String slot,
        String problemKey,
        String name,
        Integer rating,
        String sourceUrl,
        boolean isRedrawn,
        boolean checkedIn,
        Long submissionId,
        String verdict,
        Integer score
) {
}
