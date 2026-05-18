package com.whut.training.service.impl;

import com.whut.training.aspect.annotation.ServiceLog;
import com.whut.training.domain.dto.*;
import com.whut.training.domain.entity.CfProblem;
import com.whut.training.domain.entity.DailyProblem;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.entity.UserDailyStatus;
import com.whut.training.domain.entity.UserPracticeDraw;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.exception.BusinessException;
import com.whut.training.repository.DailyProblemRepository;
import com.whut.training.repository.UserRepository;
import com.whut.training.service.CodeforcesApiService;
import com.whut.training.service.DailyProblemCacheService;
import com.whut.training.service.DailyProblemService;
import com.whut.training.service.ProblemFavoriteService;
import com.whut.training.service.ProblemLikeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@ServiceLog
public class DailyProblemServiceImpl implements DailyProblemService {

    private final DailyProblemRepository dailyProblemRepository;
    private final CodeforcesApiService codeforcesApiService;
    private final Object generationLock = new Object();
    private final int defaultMinRating;
    private final int defaultMaxRating;
    private final int noRepeatDays;
    private final UserRepository userRepository;
    private final DailyProblemCacheService dailyProblemCacheService;
    private final ProblemLikeService problemLikeService;
    private final ProblemFavoriteService problemFavoriteService;

    public DailyProblemServiceImpl(
            DailyProblemRepository dailyProblemRepository,
            CodeforcesApiService codeforcesApiService,
            @Value("${app.daily-problem.min-rating:1200}") int defaultMinRating,
            @Value("${app.daily-problem.max-rating:1600}") int defaultMaxRating,
            @Value("${app.daily-problem.no-repeat-days:90}") int noRepeatDays,
            UserRepository userRepository,
            DailyProblemCacheService dailyProblemCacheService,
            ProblemLikeService problemLikeService,
            ProblemFavoriteService problemFavoriteService) {
        this.dailyProblemRepository = dailyProblemRepository;
        this.codeforcesApiService = codeforcesApiService;
        this.defaultMinRating = defaultMinRating;
        this.defaultMaxRating = defaultMaxRating;
        this.noRepeatDays = noRepeatDays;
        this.userRepository = userRepository;
        this.dailyProblemCacheService = dailyProblemCacheService;
        this.problemLikeService = problemLikeService;
        this.problemFavoriteService = problemFavoriteService;
    }

    @Scheduled(cron = "${app.daily-problem.cron:0 5 0 * * *}", zone = "${app.daily-problem.zone:Asia/Shanghai}")
    public void generateDailyProblemByScheduler() {
        ensureDailyProblem(LocalDate.now(), false, "scheduler");
    }

    @Scheduled(cron = "${app.daily-problem.sync-cron:0 0 */6 * * *}", zone = "${app.daily-problem.zone:Asia/Shanghai}")
    public void syncProblemPoolByScheduler() {
        syncProblemPool();
    }

    @Override
    public DailyProblem resolveTodayProblem() {
        return ensureDailyProblem(LocalDate.now(), false, "api");
    }

    @Override
    public DailyProblemTodayResponse getToday(User user) {
        DailyProblem dailyProblem = resolveTodayProblem();
        LocalDate today = dailyProblem.date();
        ProblemLikeSummary likeSummary = getLikeSummary(dailyProblem.problemKey(), user.getId());
        ProblemFavoriteSummary favoriteSummary = getFavoriteSummary(dailyProblem.problemKey(), user.getId());
        Optional<com.whut.training.domain.entity.UserDailyStatus> statusOptional =
                dailyProblemRepository.findUserDailyStatus(user.getId(), today);
        return new DailyProblemTodayResponse(
                toProblemView("DAILY", dailyProblem, likeSummary, favoriteSummary),
                statusOptional.isPresent(),
                statusOptional.map(com.whut.training.domain.entity.UserDailyStatus::score).orElse(0)
        );
    }

    @Override
    public CheckInResultResponse checkIn(User user, Long submissionId) {
        DailyProblem dailyProblem = resolveTodayProblem();
        LocalDate today = dailyProblem.date();
        int awardedScore = 1;
        if (dailyProblemRepository.findUserDailyStatus(user.getId(), today).isPresent()) {
            throw new BusinessException(409, "今日已打卡，请勿重复提交");
        }

        CodeforcesApiService.SubmissionStatus submissionStatus = verifySubmission(
                user.getUsername(),
                submissionId,
                dailyProblem.contestId(),
                dailyProblem.problemIndex()
        );
        if (!"OK".equalsIgnoreCase(submissionStatus.verdict())) {
            throw new BusinessException(400, "提交未通过，判题结果为 " + submissionStatus.verdict());
        }

        dailyProblemRepository.saveUserDailyStatus(
                user.getId(),
                today,
                submissionId,
                submissionStatus.verdict(),
                awardedScore
        );
        //给当前用户加上daily分数
        User persistedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new BusinessException(404, "user not found"));
        LocalDate previousCheckInDate = dailyProblemRepository.findLatestUserDailyStatusDateBefore(user.getId(), today)
                .orElse(null);
        com.whut.training.service.UserStreakCalculator.StreakSnapshot nextStreak =
                com.whut.training.service.UserStreakCalculator.nextAfterCheckIn(
                        persistedUser.getCurrentStreakDays(),
                        persistedUser.getLongestStreakDays(),
                        previousCheckInDate,
                        today
                );
        Integer userScore = (persistedUser.getScore() == null ? 0 : persistedUser.getScore()) + awardedScore;
        userRepository.updateUserScoreAndStreakStats(
                user.getId(),
                userScore,
                nextStreak.currentStreakDays(),
                nextStreak.longestStreakDays()
        );

        return new CheckInResultResponse("DAILY", true, submissionId, submissionStatus.verdict(), awardedScore);
    }

    @Override
    public List<DailyProblemHistoryItem> getHistory(User user, int limit) {
        LocalDate today = LocalDate.now();
        ensureDailyProblem(today, false, "api");
        int safeDays = Math.max(1, Math.min(60, limit));
        LocalDate startDate = today.minusDays(safeDays - 1L);
        for (LocalDate date = startDate; !date.isAfter(today); date = date.plusDays(1)) {
            ensureDailyProblem(date, false, "api");
        }
        List<DailyProblem> problems = dailyProblemRepository.findDailyProblemsByDateRange(startDate, today);
        Map<LocalDate, UserDailyStatus> statusByDate = dailyProblemRepository.findUserDailyStatusByDateRange(
                user.getId(),
                startDate,
                today
        );
        Map<String, ProblemLikeSummary> likeStats = problemLikeService.getLikeStats(
                problems.stream().map(DailyProblem::problemKey).toList(),
                user.getId()
        );
        Map<String, ProblemFavoriteSummary> favoriteStats = problemFavoriteService.getFavoriteStats(
                problems.stream().map(DailyProblem::problemKey).toList(),
                user.getId()
        );
        return problems.stream()
                .map(problem -> {
                    UserDailyStatus status = statusByDate.get(problem.date());
                    ProblemLikeSummary likeSummary = likeStats.getOrDefault(
                            problem.problemKey(),
                            new ProblemLikeSummary(problem.problemKey(), 0, false)
                    );
                    ProblemFavoriteSummary favoriteSummary = favoriteStats.getOrDefault(
                            problem.problemKey(),
                            new ProblemFavoriteSummary(problem.problemKey(), false, null)
                    );
                    return new DailyProblemHistoryItem(
                            problem.date().toString(),
                            problem.problemKey(),
                            problem.name(),
                            problem.rating(),
                            problem.sourceUrl(),
                            status != null,
                            status == null ? null : status.submissionId(),
                            status == null ? null : status.verdict(),
                            status == null ? null : status.score(),
                            likeSummary.likeCount(),
                            likeSummary.likedByMe(),
                            favoriteSummary.favoritedByMe(),
                            favoriteSummary.favoritedAt()
                    );
                })
                .toList();
    }

    @Override
    public List<PracticeHistoryItem> getPracticeHistory(User user, int limit) {
        List<PracticeHistoryItem> items = dailyProblemRepository.findCheckedPracticeHistory(user.getId(), limit);
        Map<String, ProblemLikeSummary> likeStats = problemLikeService.getLikeStats(
                items.stream().map(PracticeHistoryItem::problemKey).toList(),
                user.getId()
        );
        Map<String, ProblemFavoriteSummary> favoriteStats = problemFavoriteService.getFavoriteStats(
                items.stream().map(PracticeHistoryItem::problemKey).toList(),
                user.getId()
        );
        return items.stream()
                .map(item -> {
                    ProblemLikeSummary likeSummary = likeStats.getOrDefault(
                            item.problemKey(),
                            new ProblemLikeSummary(item.problemKey(), 0, false)
                    );
                    ProblemFavoriteSummary favoriteSummary = favoriteStats.getOrDefault(
                            item.problemKey(),
                            new ProblemFavoriteSummary(item.problemKey(), false, null)
                    );
                    return new PracticeHistoryItem(
                            item.drawId(),
                            item.drawDate(),
                            item.problemKey(),
                            item.name(),
                            item.rating(),
                            item.sourceUrl(),
                            item.submissionId(),
                            item.verdict(),
                            item.checkedAt(),
                            likeSummary.likeCount(),
                            likeSummary.likedByMe(),
                            favoriteSummary.favoritedByMe(),
                            favoriteSummary.favoritedAt()
                    );
                })
                .toList();
    }

    @Override
    public PracticeDrawResponse drawPracticeProblem(User user, Integer minRating, Integer maxRating, String tags) {
        ensureProblemPoolAvailable();
        int resolvedMinRating = minRating == null ? defaultMinRating : minRating;
        int resolvedMaxRating = maxRating == null ? defaultMaxRating : maxRating;
        if (resolvedMinRating > resolvedMaxRating) {
            throw new BusinessException(400, "invalid rating range");
        }
        List<String> tagFilters = parseTagFilters(tags);

        CfProblem problem = dailyProblemRepository.findRandomProblem(resolvedMinRating, resolvedMaxRating, tagFilters)
                .orElseThrow(() -> new BusinessException(
                        404,
                        tagFilters.isEmpty()
                                ? "no problem available for this rating range"
                                : "no problem available for this rating range and tags: " + String.join(",", tagFilters)
                ));
        UserPracticeDraw draw = dailyProblemRepository.insertPracticeDraw(user.getId(), LocalDate.now(), problem);
        ProblemLikeSummary likeSummary = getLikeSummary(draw.problemKey(), user.getId());
        ProblemFavoriteSummary favoriteSummary = getFavoriteSummary(draw.problemKey(), user.getId());
        return new PracticeDrawResponse(
                draw.id(),
                new ProblemView(
                        "PRACTICE",
                        draw.drawDate().toString(),
                        draw.problemKey(),
                        draw.contestId(),
                        draw.problemIndex(),
                        draw.name(),
                        draw.rating(),
                        draw.tags(),
                        draw.sourceUrl(),
                        likeSummary.likeCount(),
                        likeSummary.likedByMe(),
                        favoriteSummary.favoritedByMe(),
                        favoriteSummary.favoritedAt()
                )
        );
    }

    private List<String> parseTagFilters(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .map(String::toLowerCase)
                .distinct()
                .limit(10)
                .toList();
    }

    @Override
    public CheckInResultResponse checkPractice(User user, Long drawId, Long submissionId) {
        UserPracticeDraw draw = dailyProblemRepository.findPracticeDrawById(drawId, user.getId())
                .orElseThrow(() -> new BusinessException(404, "practice draw not found"));

        CodeforcesApiService.SubmissionStatus submissionStatus = verifySubmission(
                user.getUsername(),
                submissionId,
                draw.contestId(),
                draw.problemIndex()
        );
        dailyProblemRepository.updatePracticeCheck(drawId, user.getId(), submissionId, submissionStatus.verdict());
        boolean accepted = "OK".equalsIgnoreCase(submissionStatus.verdict());
        return new CheckInResultResponse("PRACTICE", accepted, submissionId, submissionStatus.verdict(), 0);
    }

    @Override
    public ProblemView regenerateTodayByAdmin(User adminUser) {
        if (adminUser == null || adminUser.getRole() != UserRole.ADMIN) {
            throw new BusinessException(403, "admin role required");
        }
        DailyProblem dailyProblem = ensureDailyProblem(LocalDate.now(), true, "admin");
        return toProblemView(
                "DAILY",
                dailyProblem,
                getLikeSummary(dailyProblem.problemKey(), adminUser.getId()),
                getFavoriteSummary(dailyProblem.problemKey(), adminUser.getId())
        );
    }

    private DailyProblem ensureDailyProblem(LocalDate date, boolean forceRegenerate, String generatedBy) {
        synchronized (generationLock) {
            if (!forceRegenerate) {
                Optional<DailyProblem> cached = dailyProblemCacheService.get(date);
                if (cached.isPresent()) {
                    return cached.get();
                }
                Optional<DailyProblem> existing = dailyProblemRepository.findDailyByDate(date);
                if (existing.isPresent()) {
                    dailyProblemCacheService.put(existing.get());
                    return existing.get();
                }
            } else {
                if (dailyProblemRepository.countDailyCheckIns(date) > 0) {
                    throw new BusinessException(409, "cannot regenerate daily problem after users have checked in");
                }
                dailyProblemCacheService.evict(date);
                dailyProblemRepository.deleteDailyByDate(date);
            }

            ensureProblemPoolAvailable();
            LocalDate noRepeatAfterDate = date.minusDays(Math.max(1, noRepeatDays));
            CfProblem problem = dailyProblemRepository
                    .findRandomProblem(defaultMinRating, defaultMaxRating, noRepeatAfterDate)
                    .or(() -> dailyProblemRepository.findRandomProblem(defaultMinRating, defaultMaxRating))
                    .orElseThrow(() -> new BusinessException(500, "failed to select daily problem"));
            DailyProblem generated = dailyProblemRepository.insertDailyProblem(date, problem, generatedBy);
            dailyProblemCacheService.put(generated);
            return generated;
        }
    }

    private void ensureProblemPoolAvailable() {
        long count = dailyProblemRepository.countProblems();
        if (count > 0) {
            return;
        }
        int synced = syncProblemPool();
        if (synced == 0) {
            throw new BusinessException(503, "failed to pull problems from codeforces");
        }
    }

    private int syncProblemPool() {
        List<CfProblem> problems = codeforcesApiService.fetchProblemSet();
        if (problems.isEmpty()) {
            return 0;
        }
        return dailyProblemRepository.upsertProblems(problems);
    }

    private CodeforcesApiService.SubmissionStatus verifySubmission(String handle, Long submissionId, Integer contestId,
                                                                   String problemIndex) {
        CodeforcesApiService.SubmissionStatus submissionStatus = codeforcesApiService
                .getSubmissionStatus(handle, submissionId)
                .orElseThrow(() -> new BusinessException(
                        400,
                        "非该用户提交记录，或提交ID不存在（submissionId=" + submissionId + "）"
                ));
        boolean sameProblem = contestId.equals(submissionStatus.contestId())
                && problemIndex.equalsIgnoreCase(submissionStatus.problemIndex());
        if (!sameProblem) {
            throw new BusinessException(
                    400,
                    "非对应题目提交，期望题目为 "
                            + contestId + problemIndex
                            + "，实际为 "
                            + submissionStatus.contestId() + submissionStatus.problemIndex()
            );
        }
        return submissionStatus;
    }

    private ProblemLikeSummary getLikeSummary(String problemKey, Long userId) {
        return problemLikeService.getLikeStats(List.of(problemKey), userId)
                .getOrDefault(problemKey, new ProblemLikeSummary(problemKey, 0, false));
    }

    private ProblemFavoriteSummary getFavoriteSummary(String problemKey, Long userId) {
        return problemFavoriteService.getFavoriteStats(List.of(problemKey), userId)
                .getOrDefault(problemKey, new ProblemFavoriteSummary(problemKey, false, null));
    }

    private ProblemView toProblemView(String type, DailyProblem dailyProblem, ProblemLikeSummary likeSummary,
                                      ProblemFavoriteSummary favoriteSummary) {
        return new ProblemView(
                type,
                dailyProblem.date().toString(),
                dailyProblem.problemKey(),
                dailyProblem.contestId(),
                dailyProblem.problemIndex(),
                dailyProblem.name(),
                dailyProblem.rating(),
                dailyProblem.tags(),
                dailyProblem.sourceUrl(),
                likeSummary.likeCount(),
                likeSummary.likedByMe(),
                favoriteSummary.favoritedByMe(),
                favoriteSummary.favoritedAt()
        );
    }
}
