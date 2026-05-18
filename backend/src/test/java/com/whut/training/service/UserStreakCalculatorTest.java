package com.whut.training.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserStreakCalculatorTest {

    @Test
    void calculateKeepsActiveStreakWhenLastCheckInIsYesterday() {
        LocalDate today = LocalDate.of(2026, 5, 17);

        UserStreakCalculator.StreakSnapshot snapshot = UserStreakCalculator.calculate(
                List.of(today.minusDays(2), today.minusDays(1)),
                today
        );

        assertEquals(2, snapshot.currentStreakDays());
        assertEquals(2, snapshot.longestStreakDays());
    }

    @Test
    void calculateZeroesExpiredCurrentStreakButKeepsLongest() {
        LocalDate today = LocalDate.of(2026, 5, 17);

        UserStreakCalculator.StreakSnapshot snapshot = UserStreakCalculator.calculate(
                List.of(
                        today.minusDays(10),
                        today.minusDays(9),
                        today.minusDays(5),
                        today.minusDays(4),
                        today.minusDays(3)
                ),
                today
        );

        assertEquals(0, snapshot.currentStreakDays());
        assertEquals(3, snapshot.longestStreakDays());
    }
}
