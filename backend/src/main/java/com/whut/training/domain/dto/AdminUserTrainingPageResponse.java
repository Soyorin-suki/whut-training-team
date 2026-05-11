package com.whut.training.domain.dto;

import java.util.List;

public record AdminUserTrainingPageResponse(
        String keyword,
        int page,
        int pageSize,
        long total,
        List<AdminUserTrainingItem> entries
) {
}
