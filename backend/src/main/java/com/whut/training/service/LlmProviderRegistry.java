package com.whut.training.service;

import com.whut.training.exception.BusinessException;
import com.whut.training.service.llm.LlmGateway;
import com.whut.training.service.llm.LlmProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class LlmProviderRegistry {

    private final LlmProperties llmProperties;
    private final Map<String, LlmGateway> gatewaysByType;

    public LlmProviderRegistry(LlmProperties llmProperties, List<LlmGateway> gateways) {
        this.llmProperties = llmProperties;
        this.gatewaysByType = gateways.stream()
                .collect(Collectors.toMap(LlmGateway::getType, Function.identity(), (left, right) -> left));
    }

    public ResolvedProvider resolve(String requestedProviderKey) {
        String providerKey = normalizeText(requestedProviderKey);
        if (providerKey == null) {
            providerKey = normalizeText(llmProperties.getDefaultProvider());
        }
        if (providerKey == null) {
            throw new BusinessException(503, "llm provider is not configured");
        }

        LlmProperties.ProviderProperties properties = llmProperties.getProviders().get(providerKey);
        if (properties == null) {
            throw new BusinessException(503, "llm provider config not found: " + providerKey);
        }
        if (properties.getBaseUrl() == null || properties.getBaseUrl().isBlank()) {
            throw new BusinessException(503, "llm provider baseUrl is not configured: " + providerKey);
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new BusinessException(503, "llm provider apiKey is not configured: " + providerKey);
        }
        if (properties.getModel() == null || properties.getModel().isBlank()) {
            throw new BusinessException(503, "llm provider model is not configured: " + providerKey);
        }

        String providerType = normalizeText(properties.getType());
        if (providerType == null) {
            providerType = "openai-compatible";
        }
        LlmGateway gateway = gatewaysByType.get(providerType);
        if (gateway == null) {
            throw new BusinessException(503, "unsupported llm provider type: " + providerType);
        }

        return new ResolvedProvider(providerKey, properties.getModel().trim(), gateway, properties);
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record ResolvedProvider(
            String providerKey,
            String modelName,
            LlmGateway gateway,
            LlmProperties.ProviderProperties properties
    ) {
    }
}
