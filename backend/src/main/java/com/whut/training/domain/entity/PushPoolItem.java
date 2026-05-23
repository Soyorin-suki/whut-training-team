package com.whut.training.domain.entity;

import java.time.LocalDateTime;

public record PushPoolItem(
        Long id,
        String title,
        String link,
        String description,
        Long submitterId,
        String status,
        Integer sortOrder,
        LocalDateTime createdAt,
        Long approvedBy,
        LocalDateTime approvedAt
) {
}
