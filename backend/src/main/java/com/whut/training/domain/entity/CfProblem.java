package com.whut.training.domain.entity;

/**
 * Codeforces 题库中的题目实体。
 *
 * @param problemKey      题目唯一键（contestId-index）。
 * @param contestId       比赛 ID。
 * @param problemIndex    题号。
 * @param name            题目标题。
 * @param rating          难度。
 * @param tags            标签，逗号分隔。
 * @param interactive     是否交互题。
 * @param sourceContestId 来源比赛 ID；非空时通常表示非标准题源。
 * @param solvedCount     通过人数。
 * @param sourceUrl       题目链接。
 */
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
