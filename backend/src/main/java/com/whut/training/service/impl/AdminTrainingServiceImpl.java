package com.whut.training.service.impl;

import com.whut.training.aspect.annotation.ServiceLog;
import com.whut.training.domain.dto.AdminDailyRecordDetailResponse;
import com.whut.training.domain.dto.AdminDailyRecordPageResponse;
import com.whut.training.domain.dto.AdminTrainingOverviewResponse;
import com.whut.training.domain.dto.AdminUserTimelineResponse;
import com.whut.training.domain.dto.AdminUserTrainingPageResponse;
import com.whut.training.domain.dto.ProblemView;
import com.whut.training.domain.entity.DailyProblem;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.exception.BusinessException;
import com.whut.training.repository.AdminTrainingRepository;
import com.whut.training.repository.DailyProblemRepository;
import com.whut.training.repository.UserRepository;
import com.whut.training.service.AdminTrainingExportWorkbookWriter;
import com.whut.training.service.AdminTrainingService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@ServiceLog
public class AdminTrainingServiceImpl implements AdminTrainingService {

    private final AdminTrainingRepository adminTrainingRepository;
    private final DailyProblemRepository dailyProblemRepository;
    private final UserRepository userRepository;
    private final AdminTrainingExportWorkbookWriter adminTrainingExportWorkbookWriter;

    public AdminTrainingServiceImpl(
            AdminTrainingRepository adminTrainingRepository,
            DailyProblemRepository dailyProblemRepository,
            UserRepository userRepository,
            AdminTrainingExportWorkbookWriter adminTrainingExportWorkbookWriter
    ) {
        this.adminTrainingRepository = adminTrainingRepository;
        this.dailyProblemRepository = dailyProblemRepository;
        this.userRepository = userRepository;
        this.adminTrainingExportWorkbookWriter = adminTrainingExportWorkbookWriter;
    }

    @Override
    public AdminTrainingOverviewResponse getOverview(User adminUser) {
        requireAdmin(adminUser);

        LocalDate today = LocalDate.now();
        long totalUsers = adminTrainingRepository.countUsers();
        long dailyCheckInCount = adminTrainingRepository.countDailyCheckIns(today);
        long practiceDrawCount = adminTrainingRepository.countPracticeDraws(today);
        long practiceCheckCount = adminTrainingRepository.countPracticeChecks(today);
        long activeUsers = adminTrainingRepository.countActiveUsers(today);
        long todayStreakUserCount = adminTrainingRepository.countTodayStreakUsers(today);
        long maxCurrentStreakDays = adminTrainingRepository.maxCurrentStreakDays();
        long maxLongestStreakDays = adminTrainingRepository.maxLongestStreakDays();
        double averageCurrentStreakDays = adminTrainingRepository.averageCurrentStreakDays();

        return new AdminTrainingOverviewResponse(
                today.toString(),
                dailyProblemRepository.findDailyByDate(today).map(this::toProblemView).orElse(null),
                totalUsers,
                activeUsers,
                dailyCheckInCount,
                Math.max(0L, totalUsers - dailyCheckInCount),
                practiceDrawCount,
                practiceCheckCount,
                todayStreakUserCount,
                maxCurrentStreakDays,
                maxLongestStreakDays,
                averageCurrentStreakDays
        );
    }

    @Override
    public AdminDailyRecordPageResponse getDailyRecords(User adminUser, String startDate, String endDate, String page, String pageSize) {
        requireAdmin(adminUser);

        LocalDate today = LocalDate.now();
        LocalDate resolvedEndDate = parseDate(endDate, today, "endDate");
        LocalDate resolvedStartDate = parseDate(startDate, resolvedEndDate.minusDays(13), "startDate");
        if (resolvedStartDate.isAfter(resolvedEndDate)) {
            throw new BusinessException(400, "startDate must not be after endDate");
        }

        int resolvedPage = parsePositiveInt(page, "page", 1);
        int resolvedPageSize = parsePositiveInt(pageSize, "pageSize", 10);
        if (resolvedPageSize > 100) {
            throw new BusinessException(400, "pageSize must be between 1 and 100");
        }

        long total = adminTrainingRepository.countDailyRecords(resolvedStartDate, resolvedEndDate);
        int offset = (resolvedPage - 1) * resolvedPageSize;
        long totalUsers = adminTrainingRepository.countUsers();

        return new AdminDailyRecordPageResponse(
                resolvedStartDate.toString(),
                resolvedEndDate.toString(),
                resolvedPage,
                resolvedPageSize,
                total,
                adminTrainingRepository.findDailyRecords(
                        resolvedStartDate,
                        resolvedEndDate,
                        resolvedPageSize,
                        offset,
                        totalUsers
                )
        );
    }

    @Override
    public AdminDailyRecordDetailResponse getDailyRecordDetail(User adminUser, String date) {
        requireAdmin(adminUser);

        LocalDate targetDate = parseDate(date, null, "date");
        DailyProblem dailyProblem = dailyProblemRepository.findDailyByDate(targetDate)
                .orElseThrow(() -> new BusinessException(404, "daily record not found"));

        return new AdminDailyRecordDetailResponse(
                targetDate.toString(),
                toProblemView(dailyProblem),
                adminTrainingRepository.findDailyCheckIns(targetDate),
                adminTrainingRepository.findDailyPracticeChecks(targetDate)
        );
    }

    @Override
    public AdminUserTrainingPageResponse getUserTrainingPage(User adminUser, String keyword, String page, String pageSize) {
        requireAdmin(adminUser);

        int resolvedPage = parsePositiveInt(page, "page", 1);
        int resolvedPageSize = parsePositiveInt(pageSize, "pageSize", 10);
        if (resolvedPageSize > 100) {
            throw new BusinessException(400, "pageSize must be between 1 and 100");
        }

        long total = adminTrainingRepository.countUserTrainingEntries(keyword);
        int offset = (resolvedPage - 1) * resolvedPageSize;

        return new AdminUserTrainingPageResponse(
                keyword == null ? "" : keyword.trim(),
                resolvedPage,
                resolvedPageSize,
                total,
                adminTrainingRepository.findUserTrainingEntries(keyword, resolvedPageSize, offset)
        );
    }

    @Override
    public AdminUserTimelineResponse getUserTimeline(User adminUser, Long userId, String limit) {
        requireAdmin(adminUser);
        if (userId == null) {
            throw new BusinessException(400, "userId is required");
        }

        int resolvedLimit = parsePositiveInt(limit, "limit", 20);
        if (resolvedLimit > 100) {
            throw new BusinessException(400, "limit must be between 1 and 100");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "user not found: " + userId));

        return new AdminUserTimelineResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getAvatarUrl(),
                user.getScore(),
                user.getSolvedProblemCount(),
                user.getHardSolvedProblemCount(),
                user.getCurrentStreakDays(),
                user.getLongestStreakDays(),
                adminTrainingRepository.findUserTimeline(userId, resolvedLimit)
        );
    }

    @Override
    public ExportPayload exportTrainingData(User adminUser) {
        requireAdmin(adminUser);

        byte[] content = adminTrainingExportWorkbookWriter.writeWorkbook(
                adminTrainingRepository.findTrainingExportRows()
        );
        return new ExportPayload(
                "admin-training-export-" + LocalDate.now() + ".xlsx",
                content
        );
    }

    private void requireAdmin(User user) {
        if (user == null) {
            throw new BusinessException(401, "unauthorized");
        }
        if (user.getRole() != UserRole.ADMIN) {
            throw new BusinessException(403, "admin role required");
        }
    }

    private LocalDate parseDate(String rawValue, LocalDate defaultValue, String fieldName) {
        if (rawValue == null || rawValue.isBlank()) {
            if (defaultValue == null) {
                throw new BusinessException(400, fieldName + " is required");
            }
            return defaultValue;
        }
        try {
            return LocalDate.parse(rawValue.trim());
        } catch (Exception ex) {
            throw new BusinessException(400, fieldName + " must be in yyyy-MM-dd format");
        }
    }

    private int parsePositiveInt(String rawValue, String fieldName, int defaultValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(rawValue.trim());
            if (parsed < 1) {
                throw new BusinessException(400, fieldName + " must be at least 1");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new BusinessException(400, fieldName + " must be a number");
        }
    }

    private ProblemView toProblemView(DailyProblem dailyProblem) {
        return new ProblemView(
                "DAILY",
                dailyProblem.date().toString(),
                dailyProblem.problemKey(),
                dailyProblem.contestId(),
                dailyProblem.problemIndex(),
                dailyProblem.name(),
                dailyProblem.rating(),
                dailyProblem.tags(),
                dailyProblem.sourceUrl(),
                0,
                false,
                false,
                null
        );
    }
}
