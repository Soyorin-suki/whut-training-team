package com.whut.training.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PracticeCheckRequest(
        @NotNull(message = "drawId is required")
        @Positive(message = "drawId must be > 0")
        Long drawId,
        @NotNull(message = "submissionId is required")
        @Positive(message = "submissionId must be > 0")
        Long submissionId
) {
}
