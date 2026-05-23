package com.whut.training.service.impl;

import com.whut.training.aspect.annotation.ServiceLog;
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
import java.util.Optional;

@Service
@ServiceLog
public class PushPoolServiceImpl implements PushPoolService {

    private final PushPoolRepository pushPoolRepository;
    private final boolean defaultAutoApprove;

    public PushPoolServiceImpl(
            PushPoolRepository pushPoolRepository,
            @Value("${app.push.defaultAutoApprove:false}") boolean defaultAutoApprove) {
        this.pushPoolRepository = pushPoolRepository;
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
        if (user.getRole() == UserRole.ADMIN) {
            return pushPoolRepository.findAll();
        }
        return pushPoolRepository.findAllByStatus("APPROVED");
    }

    @Override
    public PushPoolItem approve(User adminUser, Long id) {
        if (adminUser.getRole() != UserRole.ADMIN) {
            throw new BusinessException(403, "admin role required");
        }
        PushPoolItem item = pushPoolRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "push item not found"));
        pushPoolRepository.updateStatus(id, "APPROVED", adminUser.getId());
        return pushPoolRepository.findById(id).orElse(item);
    }

    @Override
    public PushPoolItem reject(User adminUser, Long id) {
        if (adminUser.getRole() != UserRole.ADMIN) {
            throw new BusinessException(403, "admin role required");
        }
        PushPoolItem item = pushPoolRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "push item not found"));
        pushPoolRepository.updateStatus(id, "REJECTED", adminUser.getId());
        return pushPoolRepository.findById(id).orElse(item);
    }

    @Override
    public PushPoolItem promote(User adminUser, Long id) {
        if (adminUser.getRole() != UserRole.ADMIN) {
            throw new BusinessException(403, "admin role required");
        }
        PushPoolItem item = pushPoolRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "push item not found"));
        pushPoolRepository.promoteToFront(id);
        return pushPoolRepository.findById(id).orElse(item);
    }

    @Override
    public PushSubmission submitSolution(User user, Long pushId, String submissionLink, String resultDescription) {
        PushPoolItem item = pushPoolRepository.findById(pushId)
                .orElseThrow(() -> new BusinessException(404, "push item not found"));
        if (!"APPROVED".equals(item.status())) {
            throw new BusinessException(400, "push item is not approved");
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
        if (user.getRole() == UserRole.ADMIN) {
            return pushPoolRepository.findSubmissionsByPushId(pushId);
        }
        return pushPoolRepository.findSubmissionsByUserId(user.getId());
    }

    @Override
    public Optional<PushPoolItem> getTodayPush() {
        LocalDate today = LocalDate.now();
        Optional<Long> pushId = pushPoolRepository.findDailyPushId(today);
        if (pushId.isEmpty()) {
            return Optional.empty();
        }
        return pushPoolRepository.findById(pushId.get());
    }

    @Scheduled(cron = "${app.push.daily-cron:0 1 0 * * *}", zone = "${app.daily-problem.zone:Asia/Shanghai}")
    public void publishDailyPush() {
        LocalDate today = LocalDate.now();
        Optional<PushPoolItem> next = pushPoolRepository.popNextApproved();
        if (next.isPresent()) {
            pushPoolRepository.setDailyPush(today, next.get().id());
            pushPoolRepository.updateSortOrder(next.get().id(), Integer.MAX_VALUE);
        }
    }
}
