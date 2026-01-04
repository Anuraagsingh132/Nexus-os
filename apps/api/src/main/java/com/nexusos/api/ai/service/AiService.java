package com.nexusos.api.ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AiService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final long timeoutSeconds;

    public AiService(
            ChatClient.Builder chatClientBuilder,
            VectorStore vectorStore,
            @Value("${nexusos.ai.timeout-seconds}") long timeoutSeconds) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        this.timeoutSeconds = timeoutSeconds;
    }

    public AiResult getAiResponse(UUID workspaceId, String query) {
        // Retrieve similar documents from VectorStore filtered by workspaceId
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        List<Document> similarDocs = vectorStore.similaritySearch(
            SearchRequest.query(query)
                .withTopK(5)
                .withFilterExpression(b.eq("workspaceId", workspaceId.toString()).build())
        );
        
        List<Map<String, String>> citations = similarDocs.stream()
                .map(doc -> Map.of(
                        "documentId", String.valueOf(doc.getMetadata().getOrDefault("documentId", "Unknown")),
                        "title", String.valueOf(doc.getMetadata().getOrDefault("title", "Untitled")),
                        "text", doc.getContent()
                ))
                .collect(Collectors.toList());

        String context = similarDocs.stream()
                .map(doc -> "Title: " + doc.getMetadata().getOrDefault("title", "Untitled") + "\nText: " + doc.getContent())
                .collect(Collectors.joining("\n\n"));
                
        String systemMessage = "You are a helpful assistant for Nexus OS. Use the following context to answer the user's question:\n{context}";
        
        String answer;
        try {
            answer = CompletableFuture.supplyAsync(() -> chatClient.prompt()
                    .system(s -> s.text(systemMessage).param("context", context))
                    .user(query)
                    .call()
                    .content())
                    .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .join();
        } catch (Exception e) {
            answer = "AI Assistant is currently unavailable. Please try again later.";
        }

        return new AiResult(answer, citations);
    }
    
    public record AiResult(String answer, List<Map<String, String>> citations) {}
}
