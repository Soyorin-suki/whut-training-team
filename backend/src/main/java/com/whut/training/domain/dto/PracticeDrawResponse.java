package com.whut.training.domain.dto;

/**
 * 练习题抽题响应。
 *
 * @param drawId 抽题记录 ID。
 * @param problem 抽到的题目视图。
 */
public record PracticeDrawResponse(
        Long drawId,
        ProblemView problem
) {
}
