package com.nexusos.api.ai;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

@Configuration
public class AiProviderConfig {

    @Configuration
    @ConditionalOnProperty(name = "OPENAI_API_KEY", havingValue = "mock-key", matchIfMissing = true)
    static class OllamaPrimaryConfig {
        @Bean
        @Primary
        public ChatModel primaryChatModel(OllamaChatModel ollamaChatModel) {
            return ollamaChatModel;
        }

        @Bean
        @Primary
        public EmbeddingModel primaryEmbeddingModel(OllamaEmbeddingModel ollamaEmbeddingModel) {
            return ollamaEmbeddingModel;
        }
    }

    @Configuration
    @ConditionalOnExpression("!'${OPENAI_API_KEY:mock-key}'.equals('mock-key')")
    static class OpenAiPrimaryConfig {
        @Bean
        @Primary
        public ChatModel primaryChatModel(org.springframework.ai.openai.OpenAiChatModel openAiChatModel) {
            return openAiChatModel;
        }

        @Bean
        @Primary
        public EmbeddingModel primaryEmbeddingModel(org.springframework.ai.openai.OpenAiEmbeddingModel openAiEmbeddingModel) {
            return openAiEmbeddingModel;
        }
    }
}
