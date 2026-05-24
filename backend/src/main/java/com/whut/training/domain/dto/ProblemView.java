package com.whut.training.domain.dto;

/**
 * 对外展示的题目视图。
 *
 * @param type        题目类型，通常为 DAILY 或 PRACTICE。
 * @param date        日期。
 * @param problemKey  题目唯一键。
 * @param contestId   比赛 ID。
 * @param problemIndex 题号。
 * @param name        题目标题。
 * @param rating      难度。
 * @param tags        标签。
 * @param sourceUrl   题目链接。
 */
public record ProblemView(
        String type,
        String date,
        String problemKey,
        Integer contestId,
        String problemIndex,
        String name,
        Integer rating,
        String tags,
        String sourceUrl
) {
}
