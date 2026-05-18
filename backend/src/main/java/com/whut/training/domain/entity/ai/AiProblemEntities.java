package com.whut.training.domain.entity.ai;

public final class AiProblemEntities {

    private AiProblemEntities() {
    }

    public record Session(
            Long id,
            String title,
            Long createdBy,
            String providerKey,
            String modelName,
            Integer targetRating,
            String targetTags,
            String status,
            String createdAt,
            String updatedAt
    ) {
    }

    public record Message(
            Long id,
            Long sessionId,
            String role,
            String content,
            String promptContextJson,
            String createdAt
    ) {
    }

    public record Draft(
            Long id,
            Long sessionId,
            Integer currentVersion,
            String title,
            String statementMd,
            String inputSpecMd,
            String outputSpecMd,
            String constraintMd,
            String hintMd,
            Integer rating,
            String tags,
            String sourceType,
            String checkerNoteMd,
            String normalizedProblemJson,
            String createdAt,
            String updatedAt
    ) {
    }

    public record Version(
            Long id,
            Long draftId,
            Integer versionNo,
            String generationPrompt,
            String assistantMessage,
            String llmResponseJson,
            String normalizedProblemJson,
            String createdAt
    ) {
    }

    public record Artifact(
            Long id,
            Long versionId,
            String artifactType,
            String fileName,
            String relativePath,
            String contentType,
            Long sizeBytes,
            String createdAt
    ) {
    }
}
