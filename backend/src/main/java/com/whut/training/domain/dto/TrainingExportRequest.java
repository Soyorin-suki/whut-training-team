package com.whut.training.domain.dto;

/**
 * 管理员训练数据 Excel 导出选项。
 */
public record TrainingExportRequest(
        String range,
        boolean includeDaily,
        boolean includeCodeforcesContests,
        boolean includeAtCoderContests
) {
    public ExportRange exportRange() {
        if (range == null || range.isBlank()) return ExportRange.WEEK;
        try {
            return ExportRange.valueOf(range.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return ExportRange.WEEK;
        }
    }

    public enum ExportRange {
        WEEK,
        MONTH,
        ALL
    }
}
