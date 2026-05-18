package com.whut.training.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record ProblemLikeRequest(
        @NotBlank(message = "problemKey is required")
        String problemKey
) {
}
