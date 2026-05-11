package com.whut.training.domain.dto;

public record AdminTrainingOverviewResponse(
        String date,
        ProblemView problem,
        long totalUsers,
        long activeUsers,
        long dailyCheckInCount,
        long pendingDailyUserCount,
        long practiceDrawCount,
        long practiceCheckCount,
        long todayStreakUserCount,
        long maxCurrentStreakDays,
        long maxLongestStreakDays,
        double averageCurrentStreakDays
) {
}
