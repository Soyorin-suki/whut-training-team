package com.whut.training.domain.dto;

/** Lightweight state returned while a rate-limited Codeforces check runs outside the servlet thread. */
public record DailyCheckInJobResponse(
        String jobId,
        String status,
        String message,
        Integer errorCode,
        CheckInResultResponse result
) {
}
