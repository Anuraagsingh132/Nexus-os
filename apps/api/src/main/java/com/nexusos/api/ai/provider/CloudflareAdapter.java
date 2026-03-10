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
 * Cloudflare Workers AI adapter.
 * Uses Cloudflare's OpenAI-compatible gateway endpoint.
 */
@Component
@ConditionalOnProperty(name = "nexusos.ai.cloudflare.api-key")
public class CloudflareAdapter implements AiProviderAdapter {

    private static final Logger log = LoggerFactory.getLogger(CloudflareAdapter.class);
    private final RestClient restClient;
    private final String model;
    private final ObjectMapper objectMapper;

    public CloudflareAdapter(
            @Value("${nexusos.ai.cloudflare.account-id:}") String accountId,
            @Value("${nexusos.ai.cloudflare.api-key:}") String apiKey,
            @Value("${nexusos.ai.cloudflare.model:@cf/meta/llama-3.1-8b-instruct}") String model,
            ObjectMapper objectMapper) {
        this.model = model;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.cloudflare.com/client/v4/accounts/" + accountId + "/ai")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    @Override
    public AiProviderType getType() { return AiProviderType.CLOUDFLARE_WORKERS_AI; }

    @Override
    public String generateText(String prompt, String systemPrompt) {
        try {
            // Cloudflare Workers AI supports OpenAI-compatible chat completions
            Map<String, Object> body = Map.of("model", model, "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", prompt)), "max_tokens", 2048);
            String resp = restClient.post().uri("/v1/chat/completions").body(body).retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(resp);
            return root.path("choices").path(0).path("message").path("content").asText("");
        } catch (Exception e) {
            log.error("Cloudflare generation failed", e);
            throw new RuntimeException("Cloudflare generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateToolCall(String prompt, String systemPrompt, String toolsJsonSchema) {
        String augmented = systemPrompt + "\n\nRespond ONLY with JSON. Tools:\n" + toolsJsonSchema +
                "\nFormat: {\"tool\":\"name\",\"arguments\":{...}} or {\"tool\":\"__none__\",\"arguments\":{},\"response\":\"...\"}";
        return generateText(prompt, augmented);
    }
}
