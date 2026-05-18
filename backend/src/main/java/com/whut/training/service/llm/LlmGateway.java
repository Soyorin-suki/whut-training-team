package com.whut.training.service.llm;

import com.whut.training.domain.dto.ai.AiProblemDtos;

import java.util.List;

public interface LlmGateway {

    String getType();

    DraftGenerationResult generateProblemDraft(DraftGenerationRequest request, LlmProperties.ProviderProperties providerProperties);

    record ChatMessage(String role, String content) {
    }

    record DraftGenerationRequest(
            String providerKey,
            String modelName,
            List<ChatMessage> messages
    ) {
    }

    record DraftGenerationResult(
            String assistantMessage,
            String rawResponseJson,
            AiProblemDtos.ProblemContent problemContent
    ) {
    }
}
