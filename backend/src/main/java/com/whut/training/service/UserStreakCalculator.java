package com.whut.training.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class UserStreakCalculator {

    private UserStreakCalculator() {
    }

    public static StreakSnapshot calculate(List<LocalDate> dates) {
        return calculate(dates, LocalDate.now());
    }

    public static StreakSnapshot calculate(List<LocalDate> dates, LocalDate referenceDate) {
        if (dates == null || dates.isEmpty()) {
            return new StreakSnapshot(0, 0);
        }

        List<LocalDate> orderedDates = dates.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
        if (orderedDates.isEmpty()) {
            return new StreakSnapshot(0, 0);
        }

        LocalDate safeReferenceDate = referenceDate == null ? LocalDate.now() : referenceDate;
        int runningStreak = 0;
        int longest = 0;
        LocalDate previousDate = null;
        for (LocalDate date : orderedDates) {
            if (previousDate != null && previousDate.plusDays(1).equals(date)) {
                runningStreak += 1;
            } else {
                runningStreak = 1;
            }
            longest = Math.max(longest, runningStreak);
            previousDate = date;
        }
        LocalDate lastCheckInDate = orderedDates.get(orderedDates.size() - 1);
        boolean streakIsStillActive = !lastCheckInDate.isBefore(safeReferenceDate.minusDays(1))
                && !lastCheckInDate.isAfter(safeReferenceDate);
        int current = streakIsStillActive ? runningStreak : 0;
        return new StreakSnapshot(current, longest);
    }

    public static StreakSnapshot nextAfterCheckIn(Integer currentStreakDays, Integer longestStreakDays,
                                                  LocalDate previousCheckInDate, LocalDate today) {
        int safeCurrent = currentStreakDays == null ? 0 : Math.max(0, currentStreakDays);
        int safeLongest = longestStreakDays == null ? 0 : Math.max(0, longestStreakDays);
        int nextCurrent = previousCheckInDate != null && previousCheckInDate.plusDays(1).equals(today)
                ? safeCurrent + 1
                : 1;
        int nextLongest = Math.max(safeLongest, nextCurrent);
        return new StreakSnapshot(nextCurrent, nextLongest);
    }

    public record StreakSnapshot(int currentStreakDays, int longestStreakDays) {
    }
}
