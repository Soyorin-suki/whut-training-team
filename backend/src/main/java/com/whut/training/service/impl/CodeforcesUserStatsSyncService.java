package com.whut.training.service.impl;

import com.whut.training.aspect.annotation.ServiceLog;
import com.whut.training.domain.entity.User;
import com.whut.training.repository.DailyProblemRepository;
import com.whut.training.repository.UserRepository;
import com.whut.training.service.CodeforcesApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@ServiceLog
public class CodeforcesUserStatsSyncService {

    private static final Logger log = LoggerFactory.getLogger(CodeforcesUserStatsSyncService.class);
    private static final int USER_STATUS_PAGE_SIZE = 1000;

    private final UserRepository userRepository;
    private final CodeforcesApiService codeforcesApiService;
    private final DailyProblemRepository dailyProblemRepository;
    private final int hardProblemThreshold;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public CodeforcesUserStatsSyncService(
            UserRepository userRepository,
            CodeforcesApiService codeforcesApiService,
            DailyProblemRepository dailyProblemRepository,
            @Value("${app.codeforces.hard-problem-threshold:2000}") int hardProblemThreshold
    ) {
        this.userRepository = userRepository;
        this.codeforcesApiService = codeforcesApiService;
        this.dailyProblemRepository = dailyProblemRepository;
        this.hardProblemThreshold = hardProblemThreshold;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnApplicationReady() {
        syncAllUsersOnce();
    }

    @Scheduled(
            cron = "${app.codeforces.user-stats-sync-cron:0 0 */6 * * *}",
            zone = "${app.codeforces.user-stats-sync-zone:Asia/Shanghai}"
    )
    public void syncOnSchedule() {
        syncAllUsersOnce();
    }

    public void syncAllUsersOnce() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        try {
            List<User> users = userRepository.findAll();
            for (User user : users) {
                syncSingleUser(user);
            }
        } finally {
            running.set(false);
        }
    }

    private void syncSingleUser(User user) {
        if (user == null || user.getId() == null || user.getUsername() == null || user.getUsername().isBlank()) {
            return;
        }

        String handle = user.getUsername().trim();
        if (codeforcesApiService.getUserInfo(handle).isEmpty()) {
            log.warn("skip codeforces stat sync for user {}: invalid handle or request failed", handle);
            return;
        }

        Optional<SolvedStats> statsOptional = collectSolvedStats(handle);
        if (statsOptional.isEmpty()) {
            log.warn("skip codeforces stat sync for user {}: failed to fetch submissions", handle);
            return;
        }

        SolvedStats stats = statsOptional.get();
        userRepository.updateUserSolvedProblemStats(
                user.getId(),
                stats.solvedProblemCount(),
                stats.hardSolvedProblemCount()
        );
    }

    private Optional<SolvedStats> collectSolvedStats(String handle) {
        Set<String> solvedProblemKeys = new HashSet<>();
        Map<String, Integer> problemRatings = new HashMap<>();
        int from = 1;

        while (true) {
            Optional<List<CodeforcesApiService.UserSubmission>> pageOptional =
                    codeforcesApiService.fetchUserSubmissions(handle, from, USER_STATUS_PAGE_SIZE);
            if (pageOptional.isEmpty()) {
                return Optional.empty();
            }

            List<CodeforcesApiService.UserSubmission> page = pageOptional.get();
            if (page.isEmpty()) {
                break;
            }

            for (CodeforcesApiService.UserSubmission submission : page) {
                if (submission == null || submission.submissionId() == null) {
                    continue;
                }
                if (!"OK".equalsIgnoreCase(submission.verdict())) {
                    continue;
                }

                String problemKey = submission.problemKey();
                if (problemKey == null || problemKey.isBlank()) {
                    continue;
                }

                solvedProblemKeys.add(problemKey);
                problemRatings.putIfAbsent(problemKey, submission.rating());
            }

            if (page.size() < USER_STATUS_PAGE_SIZE) {
                break;
            }
            from += USER_STATUS_PAGE_SIZE;
        }

        int hardSolvedProblemCount = 0;
        for (String problemKey : solvedProblemKeys) {
            Integer rating = problemRatings.get(problemKey);
            if (rating == null) {
                rating = dailyProblemRepository.findProblemRatingByKey(problemKey).orElse(null);
            }
            if (rating != null && rating > hardProblemThreshold) {
                hardSolvedProblemCount++;
            }
        }

        return Optional.of(new SolvedStats(solvedProblemKeys.size(), hardSolvedProblemCount));
    }

    private record SolvedStats(int solvedProblemCount, int hardSolvedProblemCount) {
    }
}
