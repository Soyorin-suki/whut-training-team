package com.whut.training.domain.dto;

import jakarta.validation.constraints.NotNull;

public record PracticeCheckRequest(@NotNull Long drawId, @NotNull Long submissionId) {
}
