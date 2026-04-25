package com.whut.training.domain.entity;

import java.time.LocalDate;

public record UserDailyStatus(
        Long userId,
        LocalDate date,
        Long submissionId,
        String verdict,
        Integer score
) {
}
