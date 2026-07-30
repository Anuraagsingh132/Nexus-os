package com.nexusos.api.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Google Gemini adapter using the Gemini REST API directly.
 * Activated when nexusos.ai.gemini.api-key is set.
 */
@Component
@ConditionalOnProperty(name = "nexusos.ai.gemini.api-key")
public class GeminiAdapter implements AiProviderAdapter {

    private static final Logger log = LoggerFactory.getLogger(GeminiAdapter.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper;

    public GeminiAdapter(
            @Value("${nexusos.ai.gemini.api-key:}") String apiKey,
            @Value("${nexusos.ai.gemini.model:gemini-2.5-flash}") String model,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public AiProviderType getType() {
        return AiProviderType.GEMINI;
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank() && !"mock-key".equals(apiKey);
    }

    @Override
    public String generateText(String prompt, String systemPrompt) {
        try {
            Map<String, Object> body = Map.of(
                    "system_instruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of("temperature", 0.7, "maxOutputTokens", 2048)
            );

            String responseJson = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            return root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
        } catch (Exception e) {
            log.error("Gemini text generation failed", e);
            throw new RuntimeException("Gemini text generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateToolCall(String prompt, String systemPrompt, String toolsJsonSchema) {
        try {
            String augmentedSystem = systemPrompt +
                    "\n\nYou MUST respond with ONLY a valid JSON object. " +
                    "If you want to call a tool, respond with: {\"tool\": \"tool_name\", \"arguments\": {...}}. " +
                    "If you want to answer directly, respond with: {\"tool\": \"__none__\", \"arguments\": {}, \"response\": \"your answer\"}. " +
                    "\n\nAvailable tools:\n" + toolsJsonSchema;

            Map<String, Object> body = Map.of(
                    "system_instruction", Map.of("parts", List.of(Map.of("text", augmentedSystem))),
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of("temperature", 0.2, "maxOutputTokens", 1024)
            );

            String responseJson = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            return root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("{}");
        } catch (Exception e) {
            log.error("Gemini tool call generation failed", e);
            throw new RuntimeException("Gemini tool call generation failed: " + e.getMessage(), e);
        }
    }
}
