package com.whut.training.domain.entity;

import java.time.LocalDateTime;

public record PushSubmission(
        Long id,
        Long pushId,
        Long userId,
        String submissionLink,
        String resultDescription,
        LocalDateTime createdAt
) {
}
