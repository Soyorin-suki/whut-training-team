package com.whut.training.domain.dto;

import java.util.List;

/** 题单详情及其一级题目列表。 */
public record ProblemListDetail(
        ProblemListSummary list,
        List<ProblemListItemView> problems
) {
}
