package com.whut.training.service.impl;

import com.whut.training.aspect.annotation.ServiceLog;
import com.whut.training.domain.entity.CfProblem;
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
    private static final int RATING_BUCKET_LOW_MIN = 800;
    private static final int RATING_BUCKET_LOW_MAX = 1400;
    private static final int RATING_BUCKET_MID_MIN = 1500;
    private static final int RATING_BUCKET_MID_MAX = 2200;
    private static final int RATING_BUCKET_HIGH_MIN = 2201;

    private final UserRepository userRepository;
    private final CodeforcesApiService codeforcesApiService;
    private final DailyProblemRepository dailyProblemRepository;
    private final boolean userStatsSyncEnabled;
    private final int hardProblemThreshold;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public CodeforcesUserStatsSyncService(
            UserRepository userRepository,
            CodeforcesApiService codeforcesApiService,
            DailyProblemRepository dailyProblemRepository,
            @Value("${app.codeforces.user-stats-sync-enabled:true}") boolean userStatsSyncEnabled,
            @Value("${app.codeforces.hard-problem-threshold:2000}") int hardProblemThreshold
    ) {
        this.userRepository = userRepository;
        this.codeforcesApiService = codeforcesApiService;
        this.dailyProblemRepository = dailyProblemRepository;
        this.userStatsSyncEnabled = userStatsSyncEnabled;
        this.hardProblemThreshold = hardProblemThreshold;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnApplicationReady() {
        if (!userStatsSyncEnabled) {
            return;
        }
        syncAllUsersOnce();
    }

    @Scheduled(
            cron = "${app.codeforces.user-stats-sync-cron:0 0 */6 * * *}",
            zone = "${app.codeforces.user-stats-sync-zone:Asia/Shanghai}"
    )
    public void syncOnSchedule() {
        if (!userStatsSyncEnabled) {
            return;
        }
        syncAllUsersOnce();
    }

    public void syncAllUsersOnce() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        try {
            List<User> users = userRepository.findAll();
            if (users.isEmpty()) {
                return;
            }
            ensureProblemPoolAvailable();
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
                stats.hardSolvedProblemCount(),
                stats.solved800To1400Count(),
                stats.solved1500To2200Count(),
                stats.solvedAbove2200Count()
        );
    }

    private void ensureProblemPoolAvailable() {
        if (dailyProblemRepository.countProblems() > 0) {
            return;
        }

        List<CfProblem> problems = codeforcesApiService.fetchProblemSet();
        if (problems.isEmpty()) {
            log.warn("skip seeding cf_problem before stats sync: failed to fetch problem set");
            return;
        }
        dailyProblemRepository.upsertProblems(problems);
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
        int solved800To1400Count = 0;
        int solved1500To2200Count = 0;
        int solvedAbove2200Count = 0;
        for (String problemKey : solvedProblemKeys) {
            Integer rating = problemRatings.get(problemKey);
            if (rating == null) {
                rating = dailyProblemRepository.findProblemRatingByKey(problemKey).orElse(null);
            }
            if (rating != null) {
                if (rating >= RATING_BUCKET_LOW_MIN && rating <= RATING_BUCKET_LOW_MAX) {
                    solved800To1400Count++;
                }
                if (rating >= RATING_BUCKET_MID_MIN && rating <= RATING_BUCKET_MID_MAX) {
                    solved1500To2200Count++;
                }
                if (rating >= RATING_BUCKET_HIGH_MIN) {
                    solvedAbove2200Count++;
                }
            }
            if (rating != null && rating > hardProblemThreshold) {
                hardSolvedProblemCount++;
            }
        }

        return Optional.of(new SolvedStats(
                solvedProblemKeys.size(),
                hardSolvedProblemCount,
                solved800To1400Count,
                solved1500To2200Count,
                solvedAbove2200Count
        ));
    }

    private record SolvedStats(
            int solvedProblemCount,
            int hardSolvedProblemCount,
            int solved800To1400Count,
            int solved1500To2200Count,
            int solvedAbove2200Count
    ) {
    }
}
