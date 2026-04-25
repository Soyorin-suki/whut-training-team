package com.whut.training.domain.entity;

import java.time.LocalDate;

public record UserPracticeDraw(
        Long id,
        Long userId,
        LocalDate drawDate,
        String problemKey,
        Integer contestId,
        String problemIndex,
        String name,
        Integer rating,
        String tags,
        String sourceUrl,
        Long submissionId,
        String verdict
) {
}
