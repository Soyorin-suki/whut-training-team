package com.whut.training.domain.dto;

/** 题单列表摘要。 */
public record ProblemListSummary(
        Long id,
        String name,
        String description,
        Long ownerUserId,
        String ownerUsername,
        String ownerDisplayName,
        boolean shared,
        int problemCount,
        boolean owner,
        String createdAt,
        String updatedAt
) {
}
