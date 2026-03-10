package com.nexusos.api.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Generic OpenAI-compatible adapter. Works with any provider exposing
 * the /v1/chat/completions endpoint (OpenAI, Groq, Cerebras, OpenRouter,
 * Together AI, NVIDIA NIM, GitHub Models, etc.).
 *
 * Configured via environment variables:
 *   OPENAI_COMPATIBLE_BASE_URL (default: https://api.groq.com/openai)
 *   OPENAI_COMPATIBLE_API_KEY
 *   OPENAI_COMPATIBLE_MODEL (default: llama-3.3-70b-versatile)
 */
@Component
public class OpenAiCompatibleAdapter implements AiProviderAdapter {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleAdapter.class);

    private final RestClient restClient;
    private final String model;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleAdapter(
            @Value("${nexusos.ai.openai-compatible.base-url:https://api.groq.com/openai}") String baseUrl,
            @Value("${nexusos.ai.openai-compatible.api-key:}") String apiKey,
            @Value("${nexusos.ai.openai-compatible.model:llama-3.3-70b-versatile}") String model,
            ObjectMapper objectMapper) {
        this.model = model;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    @Override
    public AiProviderType getType() {
        return AiProviderType.OPENAI;
    }

    @Override
    public String generateText(String prompt, String systemPrompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.7,
                    "max_tokens", 2048
            );

            String responseJson = restClient.post()
                    .uri("/v1/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            return root.path("choices").path(0).path("message").path("content").asText("");
        } catch (Exception e) {
            log.error("OpenAI-compatible text generation failed", e);
            throw new RuntimeException("OpenAI-compatible text generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateToolCall(String prompt, String systemPrompt, String toolsJsonSchema) {
        try {
            // Use the system prompt to instruct JSON tool call output
            String augmentedSystem = systemPrompt +
                    "\n\nYou MUST respond with ONLY a valid JSON object. " +
                    "If you want to call a tool, respond with: {\"tool\": \"tool_name\", \"arguments\": {...}}. " +
                    "If you want to answer directly without a tool, respond with: {\"tool\": \"__none__\", \"arguments\": {}, \"response\": \"your text answer\"}. " +
                    "\n\nAvailable tools:\n" + toolsJsonSchema;

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", augmentedSystem),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.2,
                    "max_tokens", 1024
            );

            String responseJson = restClient.post()
                    .uri("/v1/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            return root.path("choices").path(0).path("message").path("content").asText("{}");
        } catch (Exception e) {
            log.error("OpenAI-compatible tool call generation failed", e);
            throw new RuntimeException("OpenAI-compatible tool call generation failed: " + e.getMessage(), e);
        }
    }
}
