package com.whut.training.service.impl;

import com.whut.training.aspect.annotation.ServiceLog;
import com.whut.training.common.TimeProvider;
import com.whut.training.domain.entity.PushPoolItem;
import com.whut.training.domain.entity.PushSubmission;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.exception.BusinessException;
import com.whut.training.repository.PushPoolRepository;
import com.whut.training.service.PushPoolService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@ServiceLog
public class PushPoolServiceImpl implements PushPoolService {

    private final PushPoolRepository pushPoolRepository;
    private final TimeProvider timeProvider;
    private final boolean defaultAutoApprove;

    public PushPoolServiceImpl(
            PushPoolRepository pushPoolRepository,
            TimeProvider timeProvider,
            @Value("${app.push.defaultAutoApprove:false}") boolean defaultAutoApprove) {
        this.pushPoolRepository = pushPoolRepository;
        this.timeProvider = timeProvider;
        this.defaultAutoApprove = defaultAutoApprove;
    }

    @Override
    public PushPoolItem submit(User user, String title, String link, String description) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(400, "title is required");
        }
        if (link == null || link.isBlank()) {
            throw new BusinessException(400, "link is required");
        }
        PushPoolItem item = pushPoolRepository.insert(title.trim(), link.trim(),
                description == null ? null : description.trim(), user.getId());
        if (defaultAutoApprove) {
            pushPoolRepository.updateStatus(item.id(), "APPROVED", null);
        }
        return pushPoolRepository.findById(item.id())
                .orElseThrow(() -> new BusinessException(500, "failed to create push item"));
    }

    @Override
    public List<PushPoolItem> list(User user) {
        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.SUPER_ADMIN) {
            return pushPoolRepository.findAll();
        }
        // 普通用户仅可查看已推送的题目（在 daily_push 中）
        return pushPoolRepository.findPublished();
    }

    @Override
    public List<PushPoolItem> getMyItems(User user) {
        return pushPoolRepository.findAllBySubmitter(user.getId());
    }

    @Override
    public PushPoolItem approve(User adminUser, Long id) {
        if (adminUser.getRole() != UserRole.ADMIN && adminUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new BusinessException(403, "admin role required");
        }
        PushPoolItem item = pushPoolRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "push item not found"));
        if (!"PENDING".equals(item.status())) {
            throw new BusinessException(400, "only pending items can be approved");
        }
        pushPoolRepository.updateStatus(id, "APPROVED", adminUser.getId());
        return pushPoolRepository.findById(id).orElse(item);
    }

    @Override
    public PushPoolItem reject(User adminUser, Long id) {
        if (adminUser.getRole() != UserRole.ADMIN && adminUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new BusinessException(403, "admin role required");
        }
        PushPoolItem item = pushPoolRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "push item not found"));
        pushPoolRepository.updateStatus(id, "REJECTED", adminUser.getId());
        return pushPoolRepository.findById(id).orElse(item);
    }

    @Override
    public PushPoolItem promote(User adminUser, Long id) {
        if (adminUser.getRole() != UserRole.ADMIN && adminUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new BusinessException(403, "admin role required");
        }
        PushPoolItem item = pushPoolRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "push item not found"));
        pushPoolRepository.promoteToFront(id);
        return pushPoolRepository.findById(id).orElse(item);
    }

    @Override
    public boolean deletePushItem(User adminUser, Long id) {
        if (adminUser.getRole() != UserRole.ADMIN && adminUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new BusinessException(403, "admin role required");
        }
        PushPoolItem item = pushPoolRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "push item not found"));
        if (!"APPROVED".equals(item.status())) {
            throw new BusinessException(400, "only approved items can be deleted");
        }
        if (pushPoolRepository.existsInDailyPush(id)) {
            throw new BusinessException(400, "cannot delete already published item");
        }
        return pushPoolRepository.deleteById(id);
    }

    @Override
    public List<Map<String, Object>> getPushHistory() {
        return pushPoolRepository.findPushedHistory();
    }

    @Override
    public List<PushPoolItem> getPool(User adminUser) {
        if (adminUser.getRole() != UserRole.ADMIN && adminUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new BusinessException(403, "admin role required");
        }
        return pushPoolRepository.findApprovedUnpublished();
    }

    @Override
    public PushSubmission submitSolution(User user, Long pushId, String submissionLink, String resultDescription) {
        PushPoolItem item = pushPoolRepository.findById(pushId)
                .orElseThrow(() -> new BusinessException(404, "push item not found"));
        if (!"APPROVED".equals(item.status()) && !"PUBLISHED".equals(item.status())) {
            throw new BusinessException(400, "push item is not available for submission");
        }
        if (submissionLink == null || submissionLink.isBlank()) {
            throw new BusinessException(400, "submission_link is required");
        }
        return pushPoolRepository.insertSubmission(pushId, user.getId(),
                submissionLink.trim(),
                resultDescription == null ? null : resultDescription.trim());
    }

    @Override
    public List<PushSubmission> getSubmissions(User user, Long pushId) {
        PushPoolItem item = pushPoolRepository.findById(pushId)
                .orElseThrow(() -> new BusinessException(404, "push item not found"));
        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.SUPER_ADMIN) {
            return pushPoolRepository.findSubmissionsByPushId(pushId);
        }
        // 普通用户仅可查看自己在该推题下的提交
        return pushPoolRepository.findSubmissionsByPushId(pushId).stream()
                .filter(s -> s.userId().equals(user.getId()))
                .toList();
    }

    @Override
    public Optional<PushPoolItem> getTodayPush() {
        LocalDate today = timeProvider.today();
        Optional<Long> pushId = pushPoolRepository.findDailyPushId(today);
        if (pushId.isPresent()) {
            return pushPoolRepository.findById(pushId.get());
        }
        // 懒发布兜底：若今日尚未推送，立即尝试发布
        publishDailyPushForDate(today);
        pushId = pushPoolRepository.findDailyPushId(today);
        if (pushId.isEmpty()) {
            return Optional.empty();
        }
        return pushPoolRepository.findById(pushId.get());
    }

    /**
     * 定时任务：每日凌晨自动推送一道已审核题目。
     *
     * <p>默认 cron: 00:01（Asia/Shanghai），可通过 {@code app.push.daily-cron} 覆盖。
     */
    @Scheduled(cron = "${app.push.daily-cron:0 1 0 * * *}", zone = "${app.daily-problem.zone:Asia/Shanghai}")
    public void publishDailyPush() {
        publishDailyPushForDate(timeProvider.today());
    }

    /**
     * 为指定日期发布每日推题（如果池中有已审核未推送的题目）。
     *
     * <p>发布后题目状态改为 PUBLISHED，与 APPROVED（已审核待推送）区分。
     * 此方法被定时任务和懒发布兜底逻辑共用。
     *
     * @param date 推送日期
     */
    private void publishDailyPushForDate(LocalDate date) {
        Optional<PushPoolItem> next = pushPoolRepository.popNextApproved();
        if (next.isPresent()) {
            pushPoolRepository.setDailyPush(date, next.get().id());
            pushPoolRepository.updateStatus(next.get().id(), "PUBLISHED", null);
        }
    }
}
