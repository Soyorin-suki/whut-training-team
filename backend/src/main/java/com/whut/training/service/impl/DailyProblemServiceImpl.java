package com.whut.training.service.impl;

import com.whut.training.aspect.annotation.ServiceLog;
import com.whut.training.domain.dto.*;
import com.whut.training.domain.entity.CfProblem;
import com.whut.training.domain.entity.DailyProblem;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.entity.UserPracticeDraw;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.exception.BusinessException;
import com.whut.training.repository.DailyProblemRepository;
import com.whut.training.service.CodeforcesApiService;
import com.whut.training.service.DailyProblemService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 每日题与练习题服务实现。
 *
 * <p>负责从 Codeforces 拉取题库、自动生成每日题、保存打卡与练习记录。当前实现对外部 API 的失败多采用空结果回落，只有在题库不可用时才显式抛出 503；这会掩盖一部分瞬时网络问题，后续适合补充重试与告警。
 */
@Service
@ServiceLog
public class DailyProblemServiceImpl implements DailyProblemService {

    private final DailyProblemRepository dailyProblemRepository;
    private final CodeforcesApiService codeforcesApiService;
    private final com.whut.training.repository.UserRepository userRepository;
    private final Object generationLock = new Object();
    private final ConcurrentHashMap<Long, Object> userCheckinLocks = new ConcurrentHashMap<>();
    private final int defaultMinRating;
    private final int defaultMaxRating;
    private final int noRepeatDays;
    private final int ratingThreshold;

    /**
     * 创建每日题服务实现。
     *
     * @param dailyProblemRepository 题目仓储。
     * @param codeforcesApiService   Codeforces API 服务。
     * @param defaultMinRating       默认最小难度。
     * @param defaultMaxRating       默认最大难度。
     * @param noRepeatDays           避免重复命中的天数窗口。
     */
    public DailyProblemServiceImpl(
            DailyProblemRepository dailyProblemRepository,
            CodeforcesApiService codeforcesApiService,
            com.whut.training.repository.UserRepository userRepository,
            @Value("${app.daily-problem.min-rating:1200}") int defaultMinRating,
            @Value("${app.daily-problem.max-rating:1600}") int defaultMaxRating,
            @Value("${app.daily-problem.no-repeat-days:90}") int noRepeatDays,
            @Value("${app.daily.ratingThreshold:1700}") int ratingThreshold
    ) {
        this.dailyProblemRepository = dailyProblemRepository;
        this.codeforcesApiService = codeforcesApiService;
        this.userRepository = userRepository;
        this.defaultMinRating = defaultMinRating;
        this.defaultMaxRating = defaultMaxRating;
        this.noRepeatDays = noRepeatDays;
        this.ratingThreshold = ratingThreshold;
    }

    /**
     * 定时生成今日题。
     */
    @Scheduled(cron = "${app.daily-problem.cron:0 5 0 * * *}", zone = "${app.daily-problem.zone:Asia/Shanghai}")
    public void generateDailyProblemByScheduler() {
        ensureDailyProblem(LocalDate.now(), false, "scheduler");
        ensureDailySlots(LocalDate.now(), false, "scheduler");
    }

    /**
     * 定时同步题库。
     */
    @Scheduled(cron = "${app.daily-problem.sync-cron:0 0 */6 * * *}", zone = "${app.daily-problem.zone:Asia/Shanghai}")
    public void syncProblemPoolByScheduler() {
        syncProblemPool();
    }

    /**
     * 获取今日题与打卡状态（返回 easy/hard 多题位）。
     *
     * @param user 当前用户。
     * @return 今日题响应。
     */
    @Override
    public DailyProblemTodayResponse getToday(User user) {
        LocalDate today = LocalDate.now();
        ensureDailySlots(today, false, "api");

        List<com.whut.training.domain.entity.DailyProblemSlot> slots =
                dailyProblemRepository.findDailySlotsByDate(today);

        List<ProblemView> problems;
        if (slots != null && !slots.isEmpty()) {
            problems = slots.stream()
                    .map(s -> new ProblemView(
                            s.slot().toUpperCase(),
                            s.date().toString(),
                            s.problemKey(),
                            s.contestId(),
                            s.problemIndex(),
                            s.name(),
                            s.rating(),
                            s.tags(),
                            s.sourceUrl()
                    ))
                    .toList();
        } else {
            // fallback to legacy single daily problem
            DailyProblem dailyProblem = ensureDailyProblem(today, false, "api");
            problems = List.of(toProblemView("DAILY", dailyProblem));
        }

        Optional<com.whut.training.domain.entity.UserDailyStatus> statusOptional =
                dailyProblemRepository.findUserDailyStatus(user.getId(), today);
        return new DailyProblemTodayResponse(
                problems,
                statusOptional.isPresent(),
                statusOptional.map(com.whut.training.domain.entity.UserDailyStatus::score).orElse(0)
        );
    }

    /**
     * 校验并保存今日题打卡。
     *
     * @param user         当前用户。
     * @param submissionId Codeforces 提交 ID。
     * @return 打卡结果。
     */
    @Override
    public CheckInResultResponse checkIn(User user, Long submissionId) {
        LocalDate today = LocalDate.now();
        // support multi-slot daily: try to find matching slot (easy/hard) first
        ensureDailyProblem(today, false, "api");
        ensureDailySlots(today, false, "api");

        List<com.whut.training.domain.entity.DailyProblemSlot> slots = dailyProblemRepository.findDailySlotsByDate(today);

        CodeforcesApiService.SubmissionStatus submissionStatus = null;
        com.whut.training.domain.entity.DailyProblemSlot matchedSlot = null;
        if (slots != null && !slots.isEmpty()) {
            // try to match submission to one of the non-redrawn slots
            for (com.whut.training.domain.entity.DailyProblemSlot slot : slots) {
                if (slot.isRedrawn()) {
                    continue;
                }
                try {
                    submissionStatus = verifySubmission(user.getUsername(), submissionId, slot.contestId(), slot.problemIndex());
                    if ("OK".equalsIgnoreCase(submissionStatus.verdict())) {
                        matchedSlot = slot;
                        break;
                    }
                } catch (BusinessException ex) {
                    // not this slot, continue
                }
            }
        }

        // fallback to original single daily problem
        com.whut.training.domain.entity.DailyProblem single = dailyProblemRepository.findDailyByDate(today).orElse(null);
        if (matchedSlot == null && single != null) {
            try {
                submissionStatus = verifySubmission(user.getUsername(), submissionId, single.contestId(), single.problemIndex());
                if (!"OK".equalsIgnoreCase(submissionStatus.verdict())) {
                    throw new BusinessException(400, "提交未通过，判题结果为 " + submissionStatus.verdict());
                }
            } catch (BusinessException ex) {
                throw ex;
            }
        }

        if (submissionStatus == null) {
            throw new BusinessException(400, "提交未通过或不匹配今日题目");
        }

        int newScore = 0;
        if (matchedSlot != null) {
            newScore = matchedSlot.rating() == null ? 0 : matchedSlot.rating();
        } else if (single != null) {
            newScore = single.rating() == null ? 0 : single.rating();
        }

        Object userLock = userCheckinLocks.computeIfAbsent(user.getId(), k -> new Object());
        synchronized (userLock) {
            var existingOpt = dailyProblemRepository.findUserDailyStatus(user.getId(), today);
            if (existingOpt.isPresent()) {
                int oldDayScore = existingOpt.get().score();
                if (newScore <= oldDayScore) {
                    dailyProblemRepository.updateUserDailyStatus(user.getId(), today, submissionId, submissionStatus.verdict(), oldDayScore);
                    return new CheckInResultResponse(matchedSlot == null ? "DAILY" : matchedSlot.slot().toUpperCase(), true, submissionId, submissionStatus.verdict(), oldDayScore);
                } else {
                    int delta = newScore - oldDayScore;
                    dailyProblemRepository.updateUserDailyStatus(user.getId(), today, submissionId, submissionStatus.verdict(), newScore);
                    userRepository.incrementTotalPoints(user.getId(), delta);
                    return new CheckInResultResponse(matchedSlot == null ? "DAILY" : matchedSlot.slot().toUpperCase(), true, submissionId, submissionStatus.verdict(), newScore);
                }
            } else {
                dailyProblemRepository.saveUserDailyStatus(user.getId(), today, submissionId, submissionStatus.verdict(), newScore);
                userRepository.incrementTotalPoints(user.getId(), newScore);
                return new CheckInResultResponse(matchedSlot == null ? "DAILY" : matchedSlot.slot().toUpperCase(), true, submissionId, submissionStatus.verdict(), newScore);
            }
        }
    }

    /**
     * 获取用户每日题历史（基于 slot 表，包含 is_redrawn）。
     *
     * @param user 当前用户。
     * @param days 查询天数，0 表示全部。
     * @return 历史列表。
     */
    @Override
    public List<DailyProblemHistoryItem> getHistory(User user, int days) {
        LocalDate today = LocalDate.now();
        LocalDate startDate;
        if (days <= 0) {
            startDate = LocalDate.of(2020, 1, 1);
        } else {
            startDate = today.minusDays(days - 1L);
        }
        return dailyProblemRepository.findDailyHistoryForUser(user.getId(), startDate, today);
    }

    /**
     * 按难度范围抽取练习题。
     *
     * @param user      当前用户。
     * @param minRating 最小 rating。
     * @param maxRating 最大 rating。
     * @return 抽题响应。
     */
    @Override
    public PracticeDrawResponse drawPracticeProblem(User user, Integer minRating, Integer maxRating) {
        ensureProblemPoolAvailable();
        int resolvedMinRating = minRating == null ? defaultMinRating : minRating;
        int resolvedMaxRating = maxRating == null ? defaultMaxRating : maxRating;
        if (resolvedMinRating > resolvedMaxRating) {
            throw new BusinessException(400, "invalid rating range");
        }

        CfProblem problem = dailyProblemRepository.findRandomProblem(resolvedMinRating, resolvedMaxRating)
                .orElseThrow(() -> new BusinessException(404, "no problem available for this rating range"));
        UserPracticeDraw draw = dailyProblemRepository.insertPracticeDraw(user.getId(), LocalDate.now(), problem);
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
                        draw.sourceUrl()
                )
        );
    }

    /**
     * 校验练习题提交并落库。
     *
     * @param user         当前用户。
     * @param drawId       抽题记录 ID。
     * @param submissionId Codeforces 提交 ID。
     * @return 校验结果。
     */
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

    /**
     * 管理员重生成今日题。
     *
     * @param adminUser 管理员用户。
     * @return 最新题视图。
     */
    @Override
    public ProblemView regenerateTodayByAdmin(User adminUser) {
        if (adminUser == null || adminUser.getRole() != UserRole.ADMIN) {
            throw new BusinessException(403, "admin role required");
        }
        DailyProblem dailyProblem = ensureDailyProblem(LocalDate.now(), true, "admin");
        return toProblemView("DAILY", dailyProblem);
    }

    @Override
    public ProblemView adminRedrawSlot(User adminUser, java.time.LocalDate date, String slot, boolean confirm) {
        if (adminUser == null || adminUser.getRole() != UserRole.ADMIN) {
            throw new BusinessException(403, "admin role required");
        }
        LocalDate target = date == null ? LocalDate.now() : date;
        // ensure slots exist
        ensureDailySlots(target, false, "admin-redraw");

        var existingOpt = dailyProblemRepository.findSlotByDateAndSlot(target, slot);
        if (existingOpt.isPresent()) {
            // mark old slot as redrawn
            dailyProblemRepository.markSlotRedrawn(existingOpt.get().id());
        }

        // generate new problem for the slot
        ensureProblemPoolAvailable();
        LocalDate noRepeatAfterDate = target.minusDays(Math.max(1, noRepeatDays));

        CfProblem picked = null;
        if ("easy".equalsIgnoreCase(slot)) {
            Integer easyMin = defaultMinRating;
            Integer easyMax = Math.min(defaultMaxRating, ratingThreshold);
            picked = dailyProblemRepository.findRandomProblem(easyMin, easyMax, noRepeatAfterDate)
                    .or(() -> dailyProblemRepository.findRandomProblem(easyMin, easyMax))
                    .orElseThrow(() -> new BusinessException(500, "failed to select easy problem"));
        } else if ("hard".equalsIgnoreCase(slot)) {
            Integer hardMin = Math.max(defaultMinRating, ratingThreshold + 1);
            Integer hardMax = defaultMaxRating;
            picked = dailyProblemRepository.findRandomProblem(hardMin, hardMax, noRepeatAfterDate)
                    .or(() -> dailyProblemRepository.findRandomProblem(hardMin, hardMax))
                    .orElseThrow(() -> new BusinessException(500, "failed to select hard problem"));
        } else {
            throw new BusinessException(400, "invalid slot");
        }

        var newSlot = dailyProblemRepository.insertDailySlot(target, slot, picked, "admin-redraw");
        // confirm currently does not change backfill behavior; saved for future semantics
        return new ProblemView(
                slot.toUpperCase(),
                newSlot.date().toString(),
                newSlot.problemKey(),
                newSlot.contestId(),
                newSlot.problemIndex(),
                newSlot.name(),
                newSlot.rating(),
                newSlot.tags(),
                newSlot.sourceUrl()
        );
    }

    /**
     * 确保某一天存在每日题。
     *
     * @param date            日期。
     * @param forceRegenerate 是否强制重建。
     * @param generatedBy     生成来源。
     * @return 每日题记录。
     */
    private DailyProblem ensureDailyProblem(LocalDate date, boolean forceRegenerate, String generatedBy) {
        synchronized (generationLock) {
            if (!forceRegenerate) {
                Optional<DailyProblem> existing = dailyProblemRepository.findDailyByDate(date);
                if (existing.isPresent()) {
                    return existing.get();
                }
            } else {
                dailyProblemRepository.deleteDailyByDate(date);
                // also clear slot table if forcing regenerate
                dailyProblemRepository.deleteDailySlotsByDate(date);
            }

            ensureProblemPoolAvailable();
            LocalDate noRepeatAfterDate = date.minusDays(Math.max(1, noRepeatDays));
            CfProblem problem = dailyProblemRepository
                    .findRandomProblem(defaultMinRating, defaultMaxRating, noRepeatAfterDate)
                    .or(() -> dailyProblemRepository.findRandomProblem(defaultMinRating, defaultMaxRating))
                    .orElseThrow(() -> new BusinessException(500, "failed to select daily problem"));
            return dailyProblemRepository.insertDailyProblem(date, problem, generatedBy);
        }
    }

    /**
     * 确保当日的两个题位（easy/hard）存在；若不存在则生成并落库。
     */
    private void ensureDailySlots(LocalDate date, boolean forceRegenerate, String generatedBy) {
        synchronized (generationLock) {
            if (!forceRegenerate) {
                List<com.whut.training.domain.entity.DailyProblemSlot> exist = dailyProblemRepository.findDailySlotsByDate(date);
                if (exist != null && !exist.isEmpty()) {
                    return;
                }
            } else {
                dailyProblemRepository.deleteDailySlotsByDate(date);
            }

            ensureProblemPoolAvailable();
            LocalDate noRepeatAfterDate = date.minusDays(Math.max(1, noRepeatDays));

            // easy: defaultMinRating .. ratingThreshold
            Integer easyMin = defaultMinRating;
            Integer easyMax = Math.min(defaultMaxRating, ratingThreshold);
            CfProblem easy = dailyProblemRepository.findRandomProblem(easyMin, easyMax, noRepeatAfterDate)
                    .or(() -> dailyProblemRepository.findRandomProblem(easyMin, easyMax))
                    .orElse(null);

            // hard: ratingThreshold+1 .. defaultMaxRating
            Integer hardMin = Math.max(defaultMinRating, ratingThreshold + 1);
            Integer hardMax = defaultMaxRating;
            CfProblem hard = dailyProblemRepository.findRandomProblem(hardMin, hardMax, noRepeatAfterDate)
                    .or(() -> dailyProblemRepository.findRandomProblem(hardMin, hardMax))
                    .orElse(null);

            if (easy != null) {
                dailyProblemRepository.insertDailySlot(date, "easy", easy, generatedBy);
            }
            if (hard != null) {
                dailyProblemRepository.insertDailySlot(date, "hard", hard, generatedBy);
            }
        }
    }

    /**
     * 确保题库可用。
     */
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

    /**
     * 从 Codeforces 同步题库。
     *
     * @return 同步条数。
     */
    private int syncProblemPool() {
        List<CfProblem> problems = codeforcesApiService.fetchProblemSet();
        if (problems.isEmpty()) {
            return 0;
        }
        return dailyProblemRepository.upsertProblems(problems);
    }

    /**
     * 校验提交是否属于指定题目。
     *
     * @param handle       Codeforces 用户名。
     * @param submissionId 提交 ID。
     * @param contestId    题目比赛 ID。
     * @param problemIndex 题目编号。
     * @return 提交状态。
     */
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

    /**
     * 将每日题记录转换为对外视图。
     *
     * @param type         题目类型。
     * @param dailyProblem 每日题实体。
     * @return 题目视图。
     */
    private ProblemView toProblemView(String type, DailyProblem dailyProblem) {
        return new ProblemView(
                type,
                dailyProblem.date().toString(),
                dailyProblem.problemKey(),
                dailyProblem.contestId(),
                dailyProblem.problemIndex(),
                dailyProblem.name(),
                dailyProblem.rating(),
                dailyProblem.tags(),
                dailyProblem.sourceUrl()
        );
    }
}
