package com.whut.training.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProblemCommentRequest(
        @NotBlank(message = "problemKey is required")
        String problemKey,
        @NotBlank(message = "content must not be blank")
        @Size(max = 1000, message = "content length must be <= 1000")
        String content,
        Long replyCommentId
) {
}
