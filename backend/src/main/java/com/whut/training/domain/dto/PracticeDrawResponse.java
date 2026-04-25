package com.whut.training.domain.dto;

public record PracticeDrawResponse(
        Long drawId,
        ProblemView problem
) {
}
