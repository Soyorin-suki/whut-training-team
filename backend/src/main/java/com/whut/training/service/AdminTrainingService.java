package com.whut.training.service;

import com.whut.training.domain.dto.AdminDailyRecordDetailResponse;
import com.whut.training.domain.dto.AdminDailyRecordPageResponse;
import com.whut.training.domain.dto.AdminTrainingOverviewResponse;
import com.whut.training.domain.dto.AdminUserTimelineResponse;
import com.whut.training.domain.dto.AdminUserTrainingPageResponse;
import com.whut.training.domain.entity.User;

public interface AdminTrainingService {
    AdminTrainingOverviewResponse getOverview(User adminUser);

    AdminDailyRecordPageResponse getDailyRecords(User adminUser, String startDate, String endDate, String page, String pageSize);

    AdminDailyRecordDetailResponse getDailyRecordDetail(User adminUser, String date);

    AdminUserTrainingPageResponse getUserTrainingPage(User adminUser, String keyword, String page, String pageSize);

    AdminUserTimelineResponse getUserTimeline(User adminUser, Long userId, String limit);
}
