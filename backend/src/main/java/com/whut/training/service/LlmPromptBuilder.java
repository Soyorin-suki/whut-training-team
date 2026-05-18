package com.whut.training.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whut.training.domain.dto.ai.AiProblemDtos;
import com.whut.training.domain.entity.ai.AiProblemEntities;
import com.whut.training.service.llm.LlmGateway;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LlmPromptBuilder {

    private final ObjectMapper objectMapper;

    public LlmPromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildInitialUserMessage(AiProblemDtos.SessionCreateRequest request) {
        String tags = String.join(", ", request.targetTags());
        String style = normalizeText(request.problemStyle());
        String extra = normalizeText(request.extraRequirements());
        StringBuilder builder = new StringBuilder();
        builder.append("Create an original competitive programming problem draft.\n");
        builder.append("Target rating: ").append(request.targetRating()).append("\n");
        builder.append("Target tags: ").append(tags).append("\n");
        if (style != null) {
            builder.append("Preferred style: ").append(style).append("\n");
        }
        if (extra != null) {
            builder.append("Extra requirements: ").append(extra).append("\n");
        }
        builder.append("Return a complete structured problem package.");
        return builder.toString();
    }

    public String buildPromptContextJson(AiProblemDtos.SessionCreateRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    public String buildPromptContextJson(String content) {
        try {
            return objectMapper.writeValueAsString(new MessageContext(content));
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    public List<LlmGateway.ChatMessage> buildConversation(
            AiProblemEntities.Session session,
            List<AiProblemEntities.Message> messages,
            AiProblemDtos.ProblemContent currentDraft
    ) {
        List<LlmGateway.ChatMessage> conversation = new ArrayList<>();
        conversation.add(new LlmGateway.ChatMessage("system", buildSystemPrompt()));
        conversation.add(new LlmGateway.ChatMessage("system", buildSessionConstraintPrompt(session)));
        if (currentDraft != null) {
            conversation.add(new LlmGateway.ChatMessage("system", buildCurrentDraftPrompt(currentDraft)));
        }
        for (AiProblemEntities.Message message : messages) {
            conversation.add(new LlmGateway.ChatMessage(message.role(), message.content()));
        }
        return conversation;
    }

    public String buildGenerationPromptText(List<LlmGateway.ChatMessage> messages) {
        StringBuilder builder = new StringBuilder();
        for (LlmGateway.ChatMessage message : messages) {
            builder.append('[')
                    .append(message.role())
                    .append("]\n")
                    .append(message.content())
                    .append("\n\n");
        }
        return builder.toString().trim();
    }

    private String buildSystemPrompt() {
        return """
                You are helping an administrator create an original competitive programming problem package.
                Always return valid JSON only.
                The top-level JSON schema must be:
                {
                  "assistantMessage": "short natural-language summary for the admin",
                  "problem": {
                    "title": "string",
                    "statementMd": "markdown string",
                    "inputSpecMd": "markdown string",
                    "outputSpecMd": "markdown string",
                    "constraintMd": "markdown string",
                    "hintMd": "markdown string",
                    "rating": 1600,
                    "tags": ["dp", "graphs"],
                    "checkerNoteMd": "markdown string or empty",
                    "samples": [
                      { "input": "text", "output": "text", "explanation": "optional text" }
                    ],
                    "testPlanMd": "markdown string",
                    "tests": [
                      { "name": "small-random", "input": "text", "output": "text" }
                    ],
                    "originalityNotice": "brief originality disclaimer"
                  }
                }
                Requirements:
                - Create an original problem draft instead of referencing existing platforms directly.
                - Keep the structure stable and complete.
                - Samples and tests must each have non-empty input and output.
                - Use markdown for long text fields.
                - Keep assistantMessage concise and actionable.
                """;
    }

    private String buildSessionConstraintPrompt(AiProblemEntities.Session session) {
        return """
                Session constraints:
                - target rating: %s
                - target tags: %s
                - provider key: %s
                """.formatted(
                session.targetRating() == null ? "-" : session.targetRating(),
                session.targetTags() == null ? "-" : session.targetTags(),
                session.providerKey() == null ? "-" : session.providerKey()
        );
    }

    private String buildCurrentDraftPrompt(AiProblemDtos.ProblemContent currentDraft) {
        try {
            return "Current active draft JSON:\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(currentDraft);
        } catch (JsonProcessingException ex) {
            return "Current active draft exists but could not be serialized.";
        }
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record MessageContext(String content) {
    }
}
