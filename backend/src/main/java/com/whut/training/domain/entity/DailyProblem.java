package com.whut.training.domain.entity;

import java.time.LocalDate;

/**
 * 每日题快照实体。
 *
 * @param id          主键。
 * @param date        日期。
 * @param problemKey  题目唯一键。
 * @param contestId   比赛 ID。
 * @param problemIndex 题号。
 * @param name        题目标题。
 * @param rating      难度。
 * @param tags        标签。
 * @param sourceUrl   题目链接。
 */
public record DailyProblem(
        Long id,
        LocalDate date,
        String problemKey,
        Integer contestId,
        String problemIndex,
        String name,
        Integer rating,
        String tags,
        String sourceUrl
) {
}
