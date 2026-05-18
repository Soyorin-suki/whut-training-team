package com.whut.training.domain.dto.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class AiProblemDtos {

    private AiProblemDtos() {
    }

    public record SessionCreateRequest(
            String providerKey,
            @NotNull(message = "targetRating is required")
            @Min(value = 800, message = "targetRating must be between 800 and 3500")
            @Max(value = 3500, message = "targetRating must be between 800 and 3500")
            Integer targetRating,
            @NotEmpty(message = "targetTags is required")
            List<@NotBlank(message = "targetTags must not contain blank values") String> targetTags,
            String problemStyle,
            String extraRequirements
    ) {
    }

    public record MessageCreateRequest(
            @NotBlank(message = "content is required")
            String content
    ) {
    }

    public record DraftPatchRequest(
            String title,
            String statementMd,
            String inputSpecMd,
            String outputSpecMd,
            String constraintMd,
            String hintMd,
            Integer rating,
            List<String> tags,
            String checkerNoteMd,
            List<SampleItem> samples,
            String testPlanMd,
            List<TestCaseItem> tests,
            String originalityNotice
    ) {
    }

    public record SampleItem(
            String input,
            String output,
            String explanation
    ) {
    }

    public record TestCaseItem(
            String name,
            String input,
            String output
    ) {
    }

    public record ProblemContent(
            String title,
            String statementMd,
            String inputSpecMd,
            String outputSpecMd,
            String constraintMd,
            String hintMd,
            Integer rating,
            List<String> tags,
            String checkerNoteMd,
            List<SampleItem> samples,
            String testPlanMd,
            List<TestCaseItem> tests,
            String originalityNotice
    ) {
    }

    public record SessionSummary(
            Long sessionId,
            String title,
            Long createdBy,
            String providerKey,
            String modelName,
            Integer targetRating,
            List<String> targetTags,
            String status,
            Integer currentVersion,
            String createdAt,
            String updatedAt
    ) {
    }

    public record SessionPageResponse(
            int page,
            int pageSize,
            long total,
            List<SessionSummary> entries
    ) {
    }

    public record MessageView(
            Long messageId,
            String role,
            String content,
            String promptContextJson,
            String createdAt
    ) {
    }

    public record DraftView(
            Long draftId,
            Long sessionId,
            Integer currentVersion,
            String title,
            String statementMd,
            String inputSpecMd,
            String outputSpecMd,
            String constraintMd,
            String hintMd,
            Integer rating,
            List<String> tags,
            String sourceType,
            String checkerNoteMd,
            List<SampleItem> samples,
            String testPlanMd,
            List<TestCaseItem> tests,
            String originalityNotice,
            String createdAt,
            String updatedAt
    ) {
    }

    public record VersionView(
            Long versionId,
            Integer versionNo,
            boolean active,
            String generationPrompt,
            String assistantMessage,
            String createdAt
    ) {
    }

    public record ArtifactView(
            Long artifactId,
            String artifactType,
            String fileName,
            String relativePath,
            String contentType,
            Long sizeBytes,
            String createdAt,
            String contentPreview
    ) {
    }

    public record ArtifactBundleResponse(
            Long draftId,
            Integer versionNo,
            String downloadUrl,
            List<ArtifactView> items
    ) {
    }

    public record SessionDetailResponse(
            SessionSummary session,
            List<MessageView> messages,
            DraftView draft,
            List<VersionView> versions,
            ArtifactBundleResponse artifactBundle
    ) {
    }
}
