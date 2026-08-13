package com.whut.training.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PushSubmissionCreateRequest(
        @NotBlank(message = "submissionLink is required")
        @Size(max = 1000, message = "submissionLink length must be <= 1000")
        String submissionLink,
        @Size(max = 2000, message = "resultDescription length must be <= 2000")
        String resultDescription
) {
}
