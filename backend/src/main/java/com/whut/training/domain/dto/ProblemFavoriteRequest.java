package com.whut.training.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record ProblemFavoriteRequest(
        @NotBlank(message = "problemKey is required")
        String problemKey
) {
}
