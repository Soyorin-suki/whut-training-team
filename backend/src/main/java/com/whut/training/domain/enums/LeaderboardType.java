package com.whut.training.domain.enums;

import java.util.Locale;

public enum LeaderboardType {
    DAILY_TOTAL("score"),
    SOLVED_COUNT("solved_problem_count"),
    HARD_SOLVED_COUNT("hard_solved_problem_count"),
    CURRENT_STREAK("current_streak_days"),
    LONGEST_STREAK("longest_streak_days"),
    DAILY_7D(null),
    MONTHLY(null);

    private final String metricColumn;

    LeaderboardType(String metricColumn) {
        this.metricColumn = metricColumn;
    }

    public static LeaderboardType from(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("unsupported leaderboard type");
        }
        return LeaderboardType.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
    }

    public boolean isSupported() {
        return metricColumn != null;
    }

    public String metricColumn() {
        if (metricColumn == null) {
            throw new IllegalStateException("unsupported leaderboard type");
        }
        return metricColumn;
    }
}
