package com.whut.training.domain.entity;

import java.time.LocalDate;

/**
 * 多题位（slot）实体，用于支持每日多题（easy/hard）以及是否被重抽标记。
 */
public record DailyProblemSlot(
        Long id,
        LocalDate date,
        String slot,
        String problemKey,
        Integer contestId,
        String problemIndex,
        String name,
        Integer rating,
        String tags,
        String sourceUrl,
        boolean isRedrawn
) {
}
