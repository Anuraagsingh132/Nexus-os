package com.nexusos.api;

import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestAiConfig {

    @Bean
    public ChatClient.Builder chatClientBuilder() {
        return Mockito.mock(ChatClient.Builder.class);
    }

    @Bean
    public VectorStore vectorStore() {
        return Mockito.mock(VectorStore.class);
    }
}
