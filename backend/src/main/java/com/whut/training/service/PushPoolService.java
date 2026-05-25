package com.whut.training.service;

import com.whut.training.domain.entity.PushPoolItem;
import com.whut.training.domain.entity.PushSubmission;
import com.whut.training.domain.entity.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PushPoolService {
    PushPoolItem submit(User user, String title, String link, String description);

    /** 管理员返回全部推题，普通用户仅返回已推送的题目 */
    List<PushPoolItem> list(User user);

    /** 返回当前用户提交的全部推题（不限状态） */
    List<PushPoolItem> getMyItems(User user);

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
