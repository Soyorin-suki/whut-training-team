package com.whut.training.service;

import com.whut.training.domain.entity.PushPoolItem;
import com.whut.training.domain.entity.PushSubmission;
import com.whut.training.domain.entity.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PushPoolService {
    PushPoolItem submit(User user, String title, String link, String description);
    List<PushPoolItem> list(User user);
    PushPoolItem approve(User adminUser, Long id);
    PushPoolItem reject(User adminUser, Long id);
    PushPoolItem promote(User adminUser, Long id);
    boolean deletePushItem(User adminUser, Long id);
    PushSubmission submitSolution(User user, Long pushId, String submissionLink, String resultDescription);
    List<PushSubmission> getSubmissions(User user, Long pushId);
    Optional<PushPoolItem> getTodayPush();
    List<Map<String, Object>> getPushHistory();
    List<PushPoolItem> getPool(User adminUser);
}
