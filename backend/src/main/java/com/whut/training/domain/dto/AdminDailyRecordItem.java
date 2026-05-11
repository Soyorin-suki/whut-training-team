package com.whut.training.domain.dto;

public record AdminDailyRecordItem(
        String date,
        ProblemView problem,
        long totalUsers,
        long dailyCheckInCount,
        long pendingDailyUserCount,
        long practiceDrawCount,
        long practiceCheckCount
) {
}
