package com.whut.training.domain.dto;

public record CheckInResultResponse(
        String type,
        boolean accepted,
        Long submissionId,
        String verdict,
        Integer score
) {
}
