package com.whut.training.domain.dto;

public record DailyProblemTodayResponse(
        ProblemView problem,
        boolean checkedIn,
        Integer score
) {
}
