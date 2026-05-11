package com.whut.training.domain.dto;

import java.util.List;

public record AdminDailyRecordPageResponse(
        String startDate,
        String endDate,
        int page,
        int pageSize,
        long total,
        List<AdminDailyRecordItem> entries
) {
}
