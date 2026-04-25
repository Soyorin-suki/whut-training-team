package com.whut.training.domain.dto;

import jakarta.validation.constraints.NotNull;

public record DailyProblemCheckInRequest(@NotNull Long submissionId) {
}
