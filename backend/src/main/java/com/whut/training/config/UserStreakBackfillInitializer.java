package com.whut.training.config;

import com.whut.training.repository.DailyProblemRepository;
import com.whut.training.repository.UserRepository;
import com.whut.training.service.UserStreakCalculator;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@DependsOn("sqliteInitializer")
public class UserStreakBackfillInitializer {

    private final UserRepository userRepository;
    private final DailyProblemRepository dailyProblemRepository;

    public UserStreakBackfillInitializer(UserRepository userRepository, DailyProblemRepository dailyProblemRepository) {
        this.userRepository = userRepository;
        this.dailyProblemRepository = dailyProblemRepository;
    }

    @PostConstruct
    public void backfill() {
        List<Long> userIds = userRepository.findUserIdsRequiringStreakBackfill();
        if (userIds.isEmpty()) {
            return;
        }

        Map<Long, List<LocalDate>> checkInDatesByUserId = dailyProblemRepository.findAllUserDailyCheckInDates();
        for (Long userId : userIds) {
            UserStreakCalculator.StreakSnapshot snapshot = UserStreakCalculator.calculate(
                    checkInDatesByUserId.getOrDefault(userId, List.of())
            );
            userRepository.updateUserStreakStats(
                    userId,
                    snapshot.currentStreakDays(),
                    snapshot.longestStreakDays()
            );
        }
    }
}
