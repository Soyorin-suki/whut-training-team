package com.whut.training.domain.dto;

/**
 * 每日热图数据项。
 *
 * @param date       日期。
 * @param score      当日得分。
 * @param colorLevel 颜色等级 (0-4)，由得分相对强度决定。
 */
public record DailyHeatmapItem(
        String date,
        int score,
        int colorLevel
) {
}
