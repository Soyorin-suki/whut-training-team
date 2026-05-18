package com.whut.training.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whut.training.domain.dto.ai.AiProblemDtos;
import com.whut.training.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiCompatibleLlmGateway implements LlmGateway {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiCompatibleLlmGateway(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().build();
    }

    @Override
    public String getType() {
        return "openai-compatible";
    }

    @Override
    public DraftGenerationResult generateProblemDraft(DraftGenerationRequest request, LlmProperties.ProviderProperties providerProperties) {
        String endpoint = resolveEndpoint(providerProperties.getBaseUrl());
        int timeoutSeconds = providerProperties.getTimeoutSeconds() == null || providerProperties.getTimeoutSeconds() < 1
                ? 30
                : providerProperties.getTimeoutSeconds();
        try {
            String requestBody = objectMapper.writeValueAsString(buildRequestPayload(request, providerProperties));
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + providerProperties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new BusinessException(502, "llm provider request failed with status " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = extractMessageContent(root.path("choices").path(0).path("message").path("content"));
            if (content == null || content.isBlank()) {
                throw new BusinessException(502, "llm provider returned empty content");
            }

            JsonNode structured = parseStructuredJson(content);
            String assistantMessage = structured.path("assistantMessage").asText("Generated a new draft version.");
            JsonNode problemNode = structured.path("problem");
            if (problemNode.isMissingNode() || problemNode.isNull()) {
                throw new BusinessException(502, "llm provider did not return problem content");
            }
            AiProblemDtos.ProblemContent problemContent = objectMapper.treeToValue(problemNode, AiProblemDtos.ProblemContent.class);
            return new DraftGenerationResult(assistantMessage, response.body(), problemContent);
        } catch (BusinessException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(502, "failed to call llm provider");
        } catch (IOException ex) {
            throw new BusinessException(502, "failed to call llm provider");
        } catch (Exception ex) {
            throw new BusinessException(502, "invalid llm provider response");
        }
    }

    private Map<String, Object> buildRequestPayload(DraftGenerationRequest request, LlmProperties.ProviderProperties providerProperties) {
        List<Map<String, String>> messages = request.messages().stream()
                .map(message -> Map.of(
                        "role", message.role(),
                        "content", message.content()
                ))
                .toList();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", providerProperties.getModel());
        payload.put("messages", messages);
        payload.put("temperature", 0.4d);
        payload.put("response_format", Map.of("type", "json_object"));
        return payload;
    }

    private String resolveEndpoint(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        if (normalized.endsWith("/chat/completions")) {
            return normalized;
        }
        if (normalized.endsWith("/")) {
            return normalized + "chat/completions";
        }
        return normalized + "/chat/completions";
    }

    private String extractMessageContent(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return null;
        }
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : contentNode) {
                if (item.isTextual()) {
                    builder.append(item.asText());
                    continue;
                }
                JsonNode textNode = item.path("text");
                if (textNode.isTextual()) {
                    builder.append(textNode.asText());
                }
            }
            return builder.toString();
        }
        return contentNode.toString();
    }

    private JsonNode parseStructuredJson(String content) throws IOException {
        try {
            return objectMapper.readTree(content);
        } catch (IOException ex) {
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return objectMapper.readTree(content.substring(start, end + 1));
            }
            throw ex;
        }
    }
}
