package com.whut.training.domain.entity;

import java.time.LocalDate;

public record DailyProblem(
        Long id,
        LocalDate date,
        String problemKey,
        Integer contestId,
        String problemIndex,
        String name,
        Integer rating,
        String tags,
        String sourceUrl
) {
}
