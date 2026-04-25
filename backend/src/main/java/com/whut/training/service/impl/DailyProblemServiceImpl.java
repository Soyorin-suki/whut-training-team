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

@Service
@ServiceLog
public class DailyProblemServiceImpl implements DailyProblemService {

    private final DailyProblemRepository dailyProblemRepository;
    private final CodeforcesApiService codeforcesApiService;
    private final Object generationLock = new Object();
    private final int defaultMinRating;
    private final int defaultMaxRating;
    private final int noRepeatDays;

    public DailyProblemServiceImpl(
            DailyProblemRepository dailyProblemRepository,
            CodeforcesApiService codeforcesApiService,
            @Value("${app.daily-problem.min-rating:1200}") int defaultMinRating,
            @Value("${app.daily-problem.max-rating:1600}") int defaultMaxRating,
            @Value("${app.daily-problem.no-repeat-days:90}") int noRepeatDays
    ) {
        this.dailyProblemRepository = dailyProblemRepository;
        this.codeforcesApiService = codeforcesApiService;
        this.defaultMinRating = defaultMinRating;
        this.defaultMaxRating = defaultMaxRating;
        this.noRepeatDays = noRepeatDays;
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
    public DailyProblemTodayResponse getToday(User user) {
        LocalDate today = LocalDate.now();
        DailyProblem dailyProblem = ensureDailyProblem(today, false, "api");
        Optional<com.whut.training.domain.entity.UserDailyStatus> statusOptional =
                dailyProblemRepository.findUserDailyStatus(user.getId(), today);
        return new DailyProblemTodayResponse(
                toProblemView("DAILY", dailyProblem),
                statusOptional.isPresent(),
                statusOptional.map(com.whut.training.domain.entity.UserDailyStatus::score).orElse(0)
        );
    }

    @Override
    public CheckInResultResponse checkIn(User user, Long submissionId) {
        LocalDate today = LocalDate.now();
        DailyProblem dailyProblem = ensureDailyProblem(today, false, "api");
        if (dailyProblemRepository.findUserDailyStatus(user.getId(), today).isPresent()) {
            throw new BusinessException(409, "today already checked in");
        }

        CodeforcesApiService.SubmissionStatus submissionStatus = verifySubmission(
                user.getUsername(),
                submissionId,
                dailyProblem.contestId(),
                dailyProblem.problemIndex()
        );
        if (!"OK".equalsIgnoreCase(submissionStatus.verdict())) {
            throw new BusinessException(400, "submission is not accepted");
        }

        dailyProblemRepository.saveUserDailyStatus(
                user.getId(),
                today,
                submissionId,
                submissionStatus.verdict(),
                1
        );
        return new CheckInResultResponse("DAILY", true, submissionId, submissionStatus.verdict(), 1);
    }

    @Override
    public List<DailyProblemHistoryItem> getHistory(User user, int limit) {
        ensureDailyProblem(LocalDate.now(), false, "api");
        int safeLimit = Math.max(1, Math.min(60, limit));
        return dailyProblemRepository.findDailyHistoryForUser(user.getId(), safeLimit);
    }

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
        return toProblemView("DAILY", dailyProblem);
    }

    private DailyProblem ensureDailyProblem(LocalDate date, boolean forceRegenerate, String generatedBy) {
        synchronized (generationLock) {
            if (!forceRegenerate) {
                Optional<DailyProblem> existing = dailyProblemRepository.findDailyByDate(date);
                if (existing.isPresent()) {
                    return existing.get();
                }
            } else {
                dailyProblemRepository.deleteDailyByDate(date);
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
                .orElseThrow(() -> new BusinessException(400, "submission not found for this user"));
        boolean sameProblem = contestId.equals(submissionStatus.contestId())
                && problemIndex.equalsIgnoreCase(submissionStatus.problemIndex());
        if (!sameProblem) {
            throw new BusinessException(400, "submission does not match target problem");
        }
        return submissionStatus;
    }

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
