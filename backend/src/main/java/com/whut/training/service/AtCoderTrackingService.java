package com.whut.training.service;

import com.whut.training.common.TimeProvider;
import com.whut.training.domain.dto.AtCoderAbcDashboard;
import com.whut.training.domain.dto.AtCoderExemptionRequest;
import com.whut.training.domain.dto.AtCoderTrackingSettingRequest;
import com.whut.training.domain.dto.UpcomingContestItem;
import com.whut.training.exception.BusinessException;
import com.whut.training.repository.AtCoderTrackingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class AtCoderTrackingService implements AtCoderTrackingUseCase {
    private static final Logger log = LoggerFactory.getLogger(AtCoderTrackingService.class);
    private static final long RESULT_DELAY_SECONDS = 30 * 60L;
    private static final long AUTO_RETRY_SECONDS = 60 * 60L;
    private static final long DISCOVERY_WINDOW_SECONDS = 120 * 24 * 60 * 60L;
    private static final long DISCOVERY_REFRESH_SECONDS = 15 * 60L;

    private final ContestService contestService;
    private final AtCoderApiService apiService;
    private final AtCoderTrackingRepository repository;
    private final TimeProvider timeProvider;
    private volatile long lastDiscoveryAttemptSeconds = Long.MIN_VALUE;

    public AtCoderTrackingService(ContestService contestService, AtCoderApiService apiService,
                                  AtCoderTrackingRepository repository, TimeProvider timeProvider) {
        this.contestService = contestService;
        this.apiService = apiService;
        this.repository = repository;
        this.timeProvider = timeProvider;
    }

    @Scheduled(
            fixedDelayString = "${app.atcoder-tracking.fixed-delay-ms:1800000}",
            initialDelayString = "${app.atcoder-tracking.initial-delay-ms:30000}"
    )
    public void scheduledSynchronization() {
        try {
            synchronize(false);
        } catch (Exception ex) {
            log.warn("AtCoder ABC scheduled synchronization failed: {}", ex.getMessage());
        }
    }

    public void synchronize(boolean force) {
        discoverContests(force);
        long now = timeProvider.nowEpochSecond();
        for (AtCoderTrackingRepository.ContestRow contest :
                repository.findSyncCandidates(now - DISCOVERY_WINDOW_SECONDS)) {
            ensureMemberSnapshot(contest, now);
            if (now < contest.endTimeSeconds() + RESULT_DELAY_SECONDS) continue;
            if (!force && "FINALIZED".equals(contest.syncStatus())) continue;
            if (!force && !isRetryDue(contest.lastSyncAt(), now)) continue;
            syncContest(contest);
        }
    }

    @Override
    public AtCoderAbcDashboard refreshAndGet(String contestId) {
        discoverContests(true);
        AtCoderTrackingRepository.ContestRow contest = resolveContest(contestId);
        ensureMemberSnapshot(contest, timeProvider.nowEpochSecond());
        if (timeProvider.nowEpochSecond() >= contest.endTimeSeconds() + RESULT_DELAY_SECONDS) {
            syncContest(repository.findContest(contest.contestId()).orElse(contest));
        }
        return getDashboard(contest.contestId());
    }

    @Override
    public AtCoderAbcDashboard getDashboard(String contestId) {
        try {
            discoverContests(false);
        } catch (Exception ex) {
            log.debug("AtCoder contest discovery unavailable while reading dashboard: {}", ex.getMessage());
        }
        AtCoderTrackingRepository.ContestRow current = resolveContest(contestId);
        ensureMemberSnapshot(current, timeProvider.nowEpochSecond());
        List<AtCoderTrackingRepository.ContestRow> contests = repository.findRecentContests(16);
        List<AtCoderTrackingRepository.RequirementRow> rows = repository.findRequirements(current.contestId());

        int completed = 0;
        int participated = 0;
        int absent = 0;
        int unbound = 0;
        int exempted = 0;
        int errors = 0;
        List<AtCoderAbcDashboard.MemberStatus> members = new ArrayList<>();
        for (AtCoderTrackingRepository.RequirementRow row : rows) {
            String status = row.exempted() ? "EXEMPT" : defaultStatus(row, current);
            if ("COMPLETED".equals(status)) completed++;
            if (row.participated()) participated++;
            if ("ABSENT".equals(status)) absent++;
            if ("UNBOUND".equals(status)) unbound++;
            if ("EXEMPT".equals(status)) exempted++;
            if ("DATA_ERROR".equals(status)) errors++;
            members.add(new AtCoderAbcDashboard.MemberStatus(
                    row.userId(), row.username(), row.displayName(), row.avatarUrl(), row.atcoderHandle(),
                    row.exempted(), row.exemptionReason(), status, row.participated(), row.acCount(),
                    splitProblemIds(row.solvedProblemIds()), row.contestRank(), row.performance(), row.rated(),
                    row.oldRating(), row.newRating(), row.checkedAt(), row.sourceError()
            ));
        }
        int denominator = Math.max(0, rows.size() - exempted);
        double rate = denominator == 0 ? 0 : Math.round(completed * 1000.0 / denominator) / 10.0;
        return new AtCoderAbcDashboard(
                repository.getSetting(), toContest(current), contests.stream().map(this::toContest).toList(),
                new AtCoderAbcDashboard.Summary(rows.size(), completed, participated, absent, unbound, exempted, errors, rate),
                List.copyOf(members)
        );
    }

    @Override
    public AtCoderAbcDashboard updateSetting(AtCoderTrackingSettingRequest request, Long administratorId,
                                             String contestId) {
        repository.updateSetting(request.minimumAcCount(), request.graceHours(), administratorId);
        return getDashboard(contestId);
    }

    @Override
    public AtCoderAbcDashboard setExemption(String contestId, Long userId, AtCoderExemptionRequest request) {
        String reason = request.reason() == null ? null : request.reason().trim();
        if (request.exempted() && (reason == null || reason.isBlank())) {
            throw new BusinessException(400, "设置豁免时必须填写原因");
        }
        repository.setExemption(contestId, userId, request.exempted(), reason);
        return getDashboard(contestId);
    }

    private synchronized void discoverContests(boolean force) {
        long now = timeProvider.nowEpochSecond();
        if (!force && lastDiscoveryAttemptSeconds != Long.MIN_VALUE
                && now - lastDiscoveryAttemptSeconds < DISCOVERY_REFRESH_SECONDS) {
            return;
        }
        lastDiscoveryAttemptSeconds = now;
        for (UpcomingContestItem item : contestService.getAtCoderContestWindow()) {
            String id = item.contestId() == null ? "" : item.contestId().toLowerCase(Locale.ROOT);
            if (!id.matches("abc\\d+")) continue;
            long start = OffsetDateTime.parse(item.startTime()).toEpochSecond();
            if (start < now - DISCOVERY_WINDOW_SECONDS) continue;
            long end = start + Math.max(0, item.durationMinutes()) * 60L;
            repository.upsertContest(id, item.name(), start, end, item.url());
            AtCoderTrackingRepository.ContestRow contest = repository.findContest(id).orElseThrow();
            ensureMemberSnapshot(contest, now);
        }
    }

    private void ensureMemberSnapshot(AtCoderTrackingRepository.ContestRow contest, long now) {
        if (contest.memberSnapshotFrozen()) return;
        if (now >= contest.startTimeSeconds()) repository.freezeActiveMembers(contest.contestId());
        else repository.prepareActiveMembers(contest.contestId());
    }

    private void syncContest(AtCoderTrackingRepository.ContestRow contest) {
        AtCoderAbcDashboard.TrackingSetting setting = repository.getSetting();
        long now = timeProvider.nowEpochSecond();
        boolean finalDue = now >= contest.endTimeSeconds() + setting.graceHours() * 3600L;
        boolean hasDataError = false;
        for (AtCoderTrackingRepository.RequirementRow row : repository.findRequirements(contest.contestId())) {
            if (row.exempted()) continue;
            String handle = row.atcoderHandle();
            if (handle == null || handle.isBlank()) {
                repository.upsertParticipation(contest.contestId(), row.userId(), false, null, null,
                        null, null, null, null, null, "UNBOUND", null);
                continue;
            }
            try {
                AtCoderApiService.AtCoderHistoryEntry history = apiService.getHistory(handle).stream()
                        .filter(item -> contest.contestId().equalsIgnoreCase(item.contestId()))
                        .findFirst().orElse(null);
                if (history == null) {
                    repository.upsertParticipation(contest.contestId(), row.userId(), false, null, null,
                            null, null, null, 0, "", finalDue ? "ABSENT" : "PENDING", null);
                    continue;
                }
                try {
                    AtCoderApiService.AcceptedProblems accepted = apiService.getAcceptedProblems(
                            handle, contest.contestId(), contest.startTimeSeconds(), contest.endTimeSeconds());
                    String status = accepted.count() >= setting.minimumAcCount() ? "COMPLETED" : "PARTICIPATED";
                    repository.upsertParticipation(contest.contestId(), row.userId(), true, history.place(),
                            history.performance(), history.rated(), history.oldRating(), history.newRating(),
                            accepted.count(), String.join(",", accepted.problemIds()), status, null);
                } catch (Exception submissionError) {
                    hasDataError = true;
                    repository.upsertParticipation(contest.contestId(), row.userId(), true, history.place(),
                            history.performance(), history.rated(), history.oldRating(), history.newRating(),
                            null, null, "DATA_ERROR", shortError(submissionError));
                }
            } catch (Exception historyError) {
                hasDataError = true;
                repository.upsertParticipation(contest.contestId(), row.userId(), row.participated(), row.contestRank(),
                        row.performance(), row.rated(), row.oldRating(), row.newRating(), row.acCount(),
                        row.solvedProblemIds(), "DATA_ERROR", shortError(historyError));
            }
        }
        repository.markContestSynced(contest.contestId(), finalDue && !hasDataError ? "FINALIZED" : "SYNCED");
    }

    private AtCoderTrackingRepository.ContestRow resolveContest(String contestId) {
        if (contestId != null && !contestId.isBlank()) {
            return repository.findContest(contestId)
                    .orElseThrow(() -> new BusinessException(404, "ABC 场次不存在"));
        }
        long now = timeProvider.nowEpochSecond();
        List<AtCoderTrackingRepository.ContestRow> contests = repository.findRecentContests(32);
        return contests.stream().filter(item -> item.startTimeSeconds() <= now).findFirst()
                .or(() -> contests.stream().min(java.util.Comparator.comparingLong(AtCoderTrackingRepository.ContestRow::startTimeSeconds)))
                .orElseThrow(() -> new BusinessException(404, "尚未发现可跟踪的 ABC 场次"));
    }

    private boolean isRetryDue(String lastSyncAt, long now) {
        if (lastSyncAt == null || lastSyncAt.isBlank()) return true;
        try {
            return now - OffsetDateTime.parse(lastSyncAt).toEpochSecond() >= AUTO_RETRY_SECONDS;
        } catch (Exception ignored) {
            return true;
        }
    }

    private String defaultStatus(AtCoderTrackingRepository.RequirementRow row,
                                 AtCoderTrackingRepository.ContestRow contest) {
        if (row.status() != null) return row.status();
        long now = timeProvider.nowEpochSecond();
        if (now < contest.startTimeSeconds()) return row.atcoderHandle() == null ? "UNBOUND" : "UPCOMING";
        if (now < contest.endTimeSeconds() + RESULT_DELAY_SECONDS) return "PENDING";
        return row.atcoderHandle() == null ? "UNBOUND" : "PENDING";
    }

    private List<String> splitProblemIds(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(",")).filter(item -> !item.isBlank()).toList();
    }

    private AtCoderAbcDashboard.Contest toContest(AtCoderTrackingRepository.ContestRow row) {
        return new AtCoderAbcDashboard.Contest(row.contestId(), row.name(), row.startTimeSeconds(),
                row.endTimeSeconds(), row.contestUrl(), row.syncStatus(), row.lastSyncAt());
    }

    private String shortError(Exception error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) message = error.getClass().getSimpleName();
        return message.length() > 480 ? message.substring(0, 480) : message;
    }
}
