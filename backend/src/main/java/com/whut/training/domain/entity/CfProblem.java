package com.whut.training.domain.entity;

public record CfProblem(
        String problemKey,
        Integer contestId,
        String problemIndex,
        String name,
        Integer rating,
        String tags,
        boolean interactive,
        Integer sourceContestId,
        Integer solvedCount,
        String sourceUrl
) {
}
