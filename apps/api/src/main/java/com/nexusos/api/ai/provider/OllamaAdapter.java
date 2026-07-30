package com.nexusos.api.ai.provider;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Adapter for local Ollama instance. Uses Spring AI's OllamaChatModel
 * which is auto-configured by spring-ai-ollama-spring-boot-starter.
 */
@Component
@ConditionalOnBean(OllamaChatModel.class)
public class OllamaAdapter implements AiProviderAdapter {

    private final ChatClient chatClient;

    public OllamaAdapter(OllamaChatModel ollamaChatModel) {
        this.chatClient = ChatClient.builder(ollamaChatModel).build();
    }

    @Override
    public AiProviderType getType() {
        return AiProviderType.OLLAMA;
    }

    @Override
    public boolean isAvailable() {
        return true; // Local Ollama fallback candidate
    }

    @Override
    public String generateText(String prompt, String systemPrompt) {
        try {
            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(prompt)
                    .call()
                    .content();
            return response != null ? response : "";
        } catch (Exception e) {
            throw new RuntimeException("Ollama text generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateToolCall(String prompt, String systemPrompt, String toolsJsonSchema) {
        try {
            // For tool calling, embed the tools schema into the system prompt
            // and ask the model to respond with a JSON tool call
            String augmentedSystem = systemPrompt + "\n\nAvailable tools (respond with a JSON object containing 'tool' and 'arguments' keys):\n" + toolsJsonSchema;
            String response = chatClient.prompt()
                    .system(augmentedSystem)
                    .user(prompt)
                    .call()
                    .content();
            return response != null ? response : "{}";
        } catch (Exception e) {
            throw new RuntimeException("Ollama tool call generation failed: " + e.getMessage(), e);
        }
    }
}
