package com.whut.training.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DailyProblemCommentRequest(
        @NotBlank(message = "content must not be blank")
        @Size(max = 1000, message = "content length must be <= 1000")
        String content,
        Long replyCommentId,
        LocalDate dailyProblemDate,
        String problemKey
) {
    public DailyProblemCommentRequest(String content, Long replyCommentId) {
        this(content, replyCommentId, null, null);
    }

    public boolean hasDailyProblemInstanceTarget() {
        return dailyProblemDate != null || (problemKey != null && !problemKey.isBlank());
    }
}
