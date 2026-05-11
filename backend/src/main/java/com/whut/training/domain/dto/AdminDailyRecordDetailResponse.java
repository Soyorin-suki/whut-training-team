package com.whut.training.domain.dto;

import java.util.List;

public record AdminDailyRecordDetailResponse(
        String date,
        ProblemView problem,
        List<AdminDailyCheckInItem> checkIns,
        List<AdminDailyPracticeItem> practiceChecks
) {
}
