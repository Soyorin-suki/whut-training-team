package com.whut.training.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DailyProblemCheckInRequest(
        @NotNull(message = "submissionId is required")
        @Positive(message = "submissionId must be > 0")
        Long submissionId
) {
}
