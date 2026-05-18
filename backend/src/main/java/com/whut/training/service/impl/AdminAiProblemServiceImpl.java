package com.whut.training.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whut.training.aspect.annotation.ServiceLog;
import com.whut.training.domain.dto.ai.AiProblemDtos;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.entity.ai.AiProblemEntities;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.exception.BusinessException;
import com.whut.training.repository.AiProblemRepository;
import com.whut.training.service.AdminAiProblemService;
import com.whut.training.service.AiProblemArtifactService;
import com.whut.training.service.AiProblemValidator;
import com.whut.training.service.LlmPromptBuilder;
import com.whut.training.service.LlmProviderRegistry;
import com.whut.training.service.llm.LlmGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@ServiceLog
public class AdminAiProblemServiceImpl implements AdminAiProblemService {

    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_READY = "READY";
    private static final String STATUS_FAILED = "FAILED";
    private static final String SOURCE_TYPE = "AI_ORIGINAL";

    private final AiProblemRepository aiProblemRepository;
    private final LlmProviderRegistry llmProviderRegistry;
    private final LlmPromptBuilder llmPromptBuilder;
    private final AiProblemValidator aiProblemValidator;
    private final AiProblemArtifactService aiProblemArtifactService;
    private final ObjectMapper objectMapper;

    public AdminAiProblemServiceImpl(
            AiProblemRepository aiProblemRepository,
            LlmProviderRegistry llmProviderRegistry,
            LlmPromptBuilder llmPromptBuilder,
            AiProblemValidator aiProblemValidator,
            AiProblemArtifactService aiProblemArtifactService,
            ObjectMapper objectMapper
    ) {
        this.aiProblemRepository = aiProblemRepository;
        this.llmProviderRegistry = llmProviderRegistry;
        this.llmPromptBuilder = llmPromptBuilder;
        this.aiProblemValidator = aiProblemValidator;
        this.aiProblemArtifactService = aiProblemArtifactService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public AiProblemDtos.SessionDetailResponse createSession(User adminUser, AiProblemDtos.SessionCreateRequest request) {
        requireAdmin(adminUser);
        List<String> targetTags = aiProblemValidator.normalizeTags(request.targetTags(), "targetTags", true);
        LlmProviderRegistry.ResolvedProvider provider = llmProviderRegistry.resolve(request.providerKey());
        String now = Instant.now().toString();

        AiProblemEntities.Session session = aiProblemRepository.saveSession(new AiProblemEntities.Session(
                null,
                "Untitled AI Problem Session",
                adminUser.getId(),
                provider.providerKey(),
                provider.modelName(),
                request.targetRating(),
                String.join(", ", targetTags),
                STATUS_PROCESSING,
                now,
                now
        ));

        try {
            String initialMessageContent = llmPromptBuilder.buildInitialUserMessage(request);
            aiProblemRepository.saveMessage(new AiProblemEntities.Message(
                    null,
                    session.id(),
                    "user",
                    initialMessageContent,
                    llmPromptBuilder.buildPromptContextJson(request),
                    now
            ));

            GeneratedDraftResult generatedResult = generateDraftResult(session, null);
            AiProblemDtos.ProblemContent generated = generatedResult.problemContent();
            String generatedJson = writeJson(generated);
            String afterGeneration = Instant.now().toString();

            AiProblemEntities.Draft draft = aiProblemRepository.saveDraft(new AiProblemEntities.Draft(
                    null,
                    session.id(),
                    1,
                    generated.title(),
                    generated.statementMd(),
                    generated.inputSpecMd(),
                    generated.outputSpecMd(),
                    generated.constraintMd(),
                    generated.hintMd(),
                    generated.rating(),
                    String.join(", ", generated.tags()),
                    SOURCE_TYPE,
                    generated.checkerNoteMd(),
                    generatedJson,
                    afterGeneration,
                    afterGeneration
            ));

            GenerationOutput generationOutput = persistVersionAndArtifacts(session, draft, 1, generatedResult);
            aiProblemRepository.saveMessage(new AiProblemEntities.Message(
                    null,
                    session.id(),
                    "assistant",
                    generationOutput.assistantMessage(),
                    generatedJson,
                    generationOutput.createdAt()
            ));

            AiProblemEntities.Session readySession = new AiProblemEntities.Session(
                    session.id(),
                    generated.title(),
                    session.createdBy(),
                    session.providerKey(),
                    session.modelName(),
                    session.targetRating(),
                    session.targetTags(),
                    STATUS_READY,
                    session.createdAt(),
                    generationOutput.createdAt()
            );
            aiProblemRepository.updateSession(readySession);
            return buildSessionDetail(readySession, draftWithTimestamp(draft, generationOutput.createdAt()));
        } catch (RuntimeException ex) {
            aiProblemRepository.updateSession(new AiProblemEntities.Session(
                    session.id(),
                    session.title(),
                    session.createdBy(),
                    session.providerKey(),
                    session.modelName(),
                    session.targetRating(),
                    session.targetTags(),
                    STATUS_FAILED,
                    session.createdAt(),
                    Instant.now().toString()
            ));
            throw ex;
        }
    }

    @Override
    public AiProblemDtos.SessionPageResponse listSessions(User adminUser, String keyword, String status, String page, String pageSize) {
        requireAdmin(adminUser);
        int resolvedPage = parsePositiveInt(page, "page", 1);
        int resolvedPageSize = parsePositiveInt(pageSize, "pageSize", 10);
        if (resolvedPageSize > 100) {
            throw new BusinessException(400, "pageSize must be between 1 and 100");
        }

        long total = aiProblemRepository.countSessions(keyword, status);
        int offset = (resolvedPage - 1) * resolvedPageSize;
        List<AiProblemDtos.SessionSummary> entries = aiProblemRepository.findSessions(keyword, status, resolvedPageSize, offset)
                .stream()
                .map(session -> {
                    Integer currentVersion = aiProblemRepository.findDraftBySessionId(session.id())
                            .map(AiProblemEntities.Draft::currentVersion)
                            .orElse(null);
                    return toSessionSummary(session, currentVersion);
                })
                .toList();

        return new AiProblemDtos.SessionPageResponse(resolvedPage, resolvedPageSize, total, entries);
    }

    @Override
    public AiProblemDtos.SessionDetailResponse getSessionDetail(User adminUser, Long sessionId) {
        requireAdmin(adminUser);
        AiProblemEntities.Session session = requireSession(sessionId);
        AiProblemEntities.Draft draft = requireDraftBySessionId(session.id());
        return buildSessionDetail(session, draft);
    }

    @Override
    @Transactional
    public AiProblemDtos.SessionDetailResponse appendMessage(User adminUser, Long sessionId, AiProblemDtos.MessageCreateRequest request) {
        requireAdmin(adminUser);
        AiProblemEntities.Session session = requireSession(sessionId);
        AiProblemEntities.Draft draft = requireDraftBySessionId(session.id());

        String now = Instant.now().toString();
        aiProblemRepository.saveMessage(new AiProblemEntities.Message(
                null,
                session.id(),
                "user",
                request.content().trim(),
                llmPromptBuilder.buildPromptContextJson(request.content()),
                now
        ));

        try {
            GeneratedDraftResult generatedResult = generateDraftResult(session, readProblemContent(draft.normalizedProblemJson()));
            AiProblemDtos.ProblemContent generated = generatedResult.problemContent();
            int nextVersion = draft.currentVersion() + 1;
            GenerationOutput generationOutput = persistVersionAndArtifacts(session, draft, nextVersion, generatedResult);

            AiProblemEntities.Draft updatedDraft = new AiProblemEntities.Draft(
                    draft.id(),
                    draft.sessionId(),
                    nextVersion,
                    generated.title(),
                    generated.statementMd(),
                    generated.inputSpecMd(),
                    generated.outputSpecMd(),
                    generated.constraintMd(),
                    generated.hintMd(),
                    generated.rating(),
                    String.join(", ", generated.tags()),
                    draft.sourceType(),
                    generated.checkerNoteMd(),
                    writeJson(generated),
                    draft.createdAt(),
                    generationOutput.createdAt()
            );
            aiProblemRepository.updateDraft(updatedDraft);
            aiProblemRepository.saveMessage(new AiProblemEntities.Message(
                    null,
                    session.id(),
                    "assistant",
                    generationOutput.assistantMessage(),
                    updatedDraft.normalizedProblemJson(),
                    generationOutput.createdAt()
            ));
            AiProblemEntities.Session readySession = new AiProblemEntities.Session(
                    session.id(),
                    generated.title(),
                    session.createdBy(),
                    session.providerKey(),
                    session.modelName(),
                    session.targetRating(),
                    session.targetTags(),
                    STATUS_READY,
                    session.createdAt(),
                    generationOutput.createdAt()
            );
            aiProblemRepository.updateSession(readySession);
            return buildSessionDetail(readySession, updatedDraft);
        } catch (RuntimeException ex) {
            aiProblemRepository.updateSession(new AiProblemEntities.Session(
                    session.id(),
                    session.title(),
                    session.createdBy(),
                    session.providerKey(),
                    session.modelName(),
                    session.targetRating(),
                    session.targetTags(),
                    STATUS_FAILED,
                    session.createdAt(),
                    Instant.now().toString()
            ));
            throw ex;
        }
    }

    @Override
    @Transactional
    public AiProblemDtos.SessionDetailResponse activateVersion(User adminUser, Long draftId, Integer versionNo) {
        requireAdmin(adminUser);
        if (versionNo == null || versionNo < 1) {
            throw new BusinessException(400, "versionNo must be at least 1");
        }

        AiProblemEntities.Draft draft = requireDraftById(draftId);
        AiProblemEntities.Session session = requireSession(draft.sessionId());
        AiProblemEntities.Version version = aiProblemRepository.findVersionByDraftIdAndVersionNo(draft.id(), versionNo)
                .orElseThrow(() -> new BusinessException(404, "version not found"));
        AiProblemDtos.ProblemContent problemContent = readProblemContent(version.normalizedProblemJson());
        String now = Instant.now().toString();

        AiProblemEntities.Draft updatedDraft = new AiProblemEntities.Draft(
                draft.id(),
                draft.sessionId(),
                version.versionNo(),
                problemContent.title(),
                problemContent.statementMd(),
                problemContent.inputSpecMd(),
                problemContent.outputSpecMd(),
                problemContent.constraintMd(),
                problemContent.hintMd(),
                problemContent.rating(),
                String.join(", ", problemContent.tags()),
                draft.sourceType(),
                problemContent.checkerNoteMd(),
                version.normalizedProblemJson(),
                draft.createdAt(),
                now
        );
        aiProblemRepository.updateDraft(updatedDraft);
        aiProblemRepository.updateSession(new AiProblemEntities.Session(
                session.id(),
                problemContent.title(),
                session.createdBy(),
                session.providerKey(),
                session.modelName(),
                session.targetRating(),
                session.targetTags(),
                STATUS_READY,
                session.createdAt(),
                now
        ));
        return buildSessionDetail(requireSession(session.id()), updatedDraft);
    }

    @Override
    @Transactional
    public AiProblemDtos.SessionDetailResponse patchDraft(User adminUser, Long draftId, AiProblemDtos.DraftPatchRequest request) {
        requireAdmin(adminUser);
        AiProblemEntities.Draft draft = requireDraftById(draftId);
        AiProblemEntities.Session session = requireSession(draft.sessionId());
        AiProblemDtos.ProblemContent merged = aiProblemValidator.mergePatch(readProblemContent(draft.normalizedProblemJson()), request);
        String now = Instant.now().toString();

        AiProblemEntities.Draft updatedDraft = new AiProblemEntities.Draft(
                draft.id(),
                draft.sessionId(),
                draft.currentVersion(),
                merged.title(),
                merged.statementMd(),
                merged.inputSpecMd(),
                merged.outputSpecMd(),
                merged.constraintMd(),
                merged.hintMd(),
                merged.rating(),
                String.join(", ", merged.tags()),
                draft.sourceType(),
                merged.checkerNoteMd(),
                writeJson(merged),
                draft.createdAt(),
                now
        );
        aiProblemRepository.updateDraft(updatedDraft);
        aiProblemRepository.updateSession(new AiProblemEntities.Session(
                session.id(),
                merged.title(),
                session.createdBy(),
                session.providerKey(),
                session.modelName(),
                session.targetRating(),
                session.targetTags(),
                session.status(),
                session.createdAt(),
                now
        ));
        return buildSessionDetail(requireSession(session.id()), updatedDraft);
    }

    @Override
    @Transactional
    public AiProblemDtos.ArtifactBundleResponse regenerateArtifacts(User adminUser, Long draftId) {
        requireAdmin(adminUser);
        AiProblemEntities.Draft draft = requireDraftById(draftId);
        AiProblemEntities.Session session = requireSession(draft.sessionId());
        AiProblemEntities.Version version = requireCurrentVersion(draft);
        AiProblemDtos.ProblemContent problemContent = readProblemContent(draft.normalizedProblemJson());
        List<AiProblemEntities.Artifact> artifacts = aiProblemArtifactService.materializeArtifacts(session, draft, version, problemContent);
        aiProblemRepository.replaceArtifacts(version.id(), artifacts);
        return buildArtifactBundle(draft, version);
    }

    @Override
    public AiProblemDtos.ArtifactBundleResponse getArtifacts(User adminUser, Long draftId) {
        requireAdmin(adminUser);
        AiProblemEntities.Draft draft = requireDraftById(draftId);
        AiProblemEntities.Version version = requireCurrentVersion(draft);
        return buildArtifactBundle(draft, version);
    }

    @Override
    public DownloadPayload getDraftZip(User adminUser, Long draftId) {
        requireAdmin(adminUser);
        AiProblemEntities.Draft draft = requireDraftById(draftId);
        AiProblemEntities.Version version = requireCurrentVersion(draft);
        byte[] bytes = aiProblemArtifactService.loadZipBytes(draft.sessionId(), draft.id(), version.versionNo());
        String fileName = "problem-" + draft.id() + "-v" + version.versionNo() + ".zip";
        return new DownloadPayload(fileName, bytes);
    }

    private GenerationOutput persistVersionAndArtifacts(
            AiProblemEntities.Session session,
            AiProblemEntities.Draft draft,
            int versionNo,
            GeneratedDraftResult generatedResult
    ) {
        AiProblemDtos.ProblemContent generated = generatedResult.problemContent();
        List<AiProblemEntities.Message> messages = aiProblemRepository.findMessagesBySessionId(session.id());
        AiProblemDtos.ProblemContent currentDraft = draft == null ? null : readProblemContent(draft.normalizedProblemJson());
        List<LlmGateway.ChatMessage> conversation = llmPromptBuilder.buildConversation(session, messages, currentDraft);
        String now = Instant.now().toString();
        AiProblemEntities.Version savedVersion = aiProblemRepository.saveVersion(new AiProblemEntities.Version(
                null,
                draft.id(),
                versionNo,
                generatedResult.generationPrompt(),
                generatedResult.assistantMessage(),
                generatedResult.rawResponseJson(),
                writeJson(generated),
                now
        ));
        List<AiProblemEntities.Artifact> artifacts = aiProblemArtifactService.materializeArtifacts(session, draft, savedVersion, generated);
        aiProblemRepository.replaceArtifacts(savedVersion.id(), artifacts);
        return new GenerationOutput(savedVersion, generatedResult.assistantMessage(), generated, now);
    }

    private GeneratedDraftResult generateDraftResult(AiProblemEntities.Session session, AiProblemDtos.ProblemContent currentDraft) {
        List<AiProblemEntities.Message> messages = aiProblemRepository.findMessagesBySessionId(session.id());
        List<LlmGateway.ChatMessage> conversation = llmPromptBuilder.buildConversation(session, messages, currentDraft);
        LlmProviderRegistry.ResolvedProvider provider = llmProviderRegistry.resolve(session.providerKey());
        LlmGateway.DraftGenerationResult result = provider.gateway().generateProblemDraft(
                new LlmGateway.DraftGenerationRequest(session.providerKey(), session.modelName(), conversation),
                provider.properties()
        );
        return new GeneratedDraftResult(
                result.assistantMessage(),
                aiProblemValidator.normalizeProblemContent(result.problemContent()),
                result.rawResponseJson(),
                llmPromptBuilder.buildGenerationPromptText(conversation)
        );
    }

    private AiProblemDtos.SessionDetailResponse buildSessionDetail(AiProblemEntities.Session session, AiProblemEntities.Draft draft) {
        List<AiProblemEntities.Message> messages = aiProblemRepository.findMessagesBySessionId(session.id());
        List<AiProblemEntities.Version> versions = aiProblemRepository.findVersionsByDraftId(draft.id());
        AiProblemEntities.Version currentVersion = requireCurrentVersion(draft);
        AiProblemDtos.ArtifactBundleResponse artifactBundle = buildArtifactBundle(draft, currentVersion);
        AiProblemDtos.ProblemContent problemContent = readProblemContent(draft.normalizedProblemJson());

        List<AiProblemDtos.VersionView> versionViews = versions.stream()
                .map(version -> new AiProblemDtos.VersionView(
                        version.id(),
                        version.versionNo(),
                        version.versionNo().equals(draft.currentVersion()),
                        version.generationPrompt(),
                        version.assistantMessage(),
                        version.createdAt()
                ))
                .toList();

        List<AiProblemDtos.MessageView> messageViews = messages.stream()
                .map(message -> new AiProblemDtos.MessageView(
                        message.id(),
                        message.role(),
                        message.content(),
                        message.promptContextJson(),
                        message.createdAt()
                ))
                .toList();

        AiProblemDtos.DraftView draftView = new AiProblemDtos.DraftView(
                draft.id(),
                draft.sessionId(),
                draft.currentVersion(),
                draft.title(),
                draft.statementMd(),
                draft.inputSpecMd(),
                draft.outputSpecMd(),
                draft.constraintMd(),
                draft.hintMd(),
                draft.rating(),
                problemContent.tags(),
                draft.sourceType(),
                draft.checkerNoteMd(),
                problemContent.samples(),
                problemContent.testPlanMd(),
                problemContent.tests(),
                problemContent.originalityNotice(),
                draft.createdAt(),
                draft.updatedAt()
        );

        return new AiProblemDtos.SessionDetailResponse(
                toSessionSummary(session, draft.currentVersion()),
                messageViews,
                draftView,
                versionViews,
                artifactBundle
        );
    }

    private AiProblemDtos.ArtifactBundleResponse buildArtifactBundle(AiProblemEntities.Draft draft, AiProblemEntities.Version version) {
        List<AiProblemEntities.Artifact> artifacts = aiProblemRepository.findArtifactsByVersionId(version.id());
        return aiProblemArtifactService.buildArtifactBundle(draft, version, artifacts);
    }

    private AiProblemDtos.SessionSummary toSessionSummary(AiProblemEntities.Session session, Integer currentVersion) {
        return new AiProblemDtos.SessionSummary(
                session.id(),
                session.title(),
                session.createdBy(),
                session.providerKey(),
                session.modelName(),
                session.targetRating(),
                splitTags(session.targetTags()),
                session.status(),
                currentVersion,
                session.createdAt(),
                session.updatedAt()
        );
    }

    private AiProblemEntities.Session requireSession(Long sessionId) {
        if (sessionId == null) {
            throw new BusinessException(400, "sessionId is required");
        }
        return aiProblemRepository.findSessionById(sessionId)
                .orElseThrow(() -> new BusinessException(404, "session not found"));
    }

    private AiProblemEntities.Draft requireDraftBySessionId(Long sessionId) {
        return aiProblemRepository.findDraftBySessionId(sessionId)
                .orElseThrow(() -> new BusinessException(404, "draft not found"));
    }

    private AiProblemEntities.Draft requireDraftById(Long draftId) {
        if (draftId == null) {
            throw new BusinessException(400, "draftId is required");
        }
        return aiProblemRepository.findDraftById(draftId)
                .orElseThrow(() -> new BusinessException(404, "draft not found"));
    }

    private AiProblemEntities.Version requireCurrentVersion(AiProblemEntities.Draft draft) {
        return aiProblemRepository.findVersionByDraftIdAndVersionNo(draft.id(), draft.currentVersion())
                .orElseThrow(() -> new BusinessException(404, "current version not found"));
    }

    private AiProblemDtos.ProblemContent readProblemContent(String json) {
        try {
            return objectMapper.readValue(json, AiProblemDtos.ProblemContent.class);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500, "failed to read draft content");
        }
    }

    private String writeJson(AiProblemDtos.ProblemContent problemContent) {
        try {
            return objectMapper.writeValueAsString(problemContent);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500, "failed to serialize draft content");
        }
    }

    private List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String part : tags.split(",")) {
            String value = part == null ? null : part.trim();
            if (value != null && !value.isEmpty()) {
                result.add(value);
            }
        }
        return result;
    }

    private AiProblemEntities.Draft draftWithTimestamp(AiProblemEntities.Draft draft, String updatedAt) {
        return new AiProblemEntities.Draft(
                draft.id(),
                draft.sessionId(),
                draft.currentVersion(),
                draft.title(),
                draft.statementMd(),
                draft.inputSpecMd(),
                draft.outputSpecMd(),
                draft.constraintMd(),
                draft.hintMd(),
                draft.rating(),
                draft.tags(),
                draft.sourceType(),
                draft.checkerNoteMd(),
                draft.normalizedProblemJson(),
                draft.createdAt(),
                updatedAt
        );
    }

    private void requireAdmin(User user) {
        if (user == null) {
            throw new BusinessException(401, "unauthorized");
        }
        if (user.getRole() != UserRole.ADMIN) {
            throw new BusinessException(403, "admin role required");
        }
    }

    private int parsePositiveInt(String rawValue, String fieldName, int defaultValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(rawValue.trim());
            if (parsed < 1) {
                throw new BusinessException(400, fieldName + " must be at least 1");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new BusinessException(400, fieldName + " must be a number");
        }
    }

    private record GenerationOutput(
            AiProblemEntities.Version version,
            String assistantMessage,
            AiProblemDtos.ProblemContent problemContent,
            String createdAt
    ) {
    }

    private record GeneratedDraftResult(
            String assistantMessage,
            AiProblemDtos.ProblemContent problemContent,
            String rawResponseJson,
            String generationPrompt
    ) {
    }
}
