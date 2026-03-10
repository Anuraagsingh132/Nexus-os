package com.nexusos.api.ai.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TaskRouter {

    private static final Logger log = LoggerFactory.getLogger(TaskRouter.class);

    private final Map<AiProviderType, AiProviderAdapter> adapters;

    // Explicit, ordered failover priority chains per task type
    private static final List<AiProviderType> AGENT_TOOL_FAILOVER = List.of(
            AiProviderType.OPENAI,
            AiProviderType.GROQ,
            AiProviderType.CEREBRAS,
            AiProviderType.OPENROUTER,
            AiProviderType.GEMINI,
            AiProviderType.OLLAMA
    );

    private static final List<AiProviderType> RAG_QA_FAILOVER = List.of(
            AiProviderType.GEMINI,
            AiProviderType.OPENAI,
            AiProviderType.GROQ,
            AiProviderType.MISTRAL,
            AiProviderType.OLLAMA
    );

    private static final List<AiProviderType> SUMMARIZATION_FAILOVER = List.of(
            AiProviderType.OLLAMA,
            AiProviderType.GEMINI,
            AiProviderType.OPENAI,
            AiProviderType.GROQ,
            AiProviderType.COHERE
    );

    private static final List<AiProviderType> EMBEDDINGS_FAILOVER = List.of(
            AiProviderType.HUGGING_FACE,
            AiProviderType.OLLAMA,
            AiProviderType.OPENAI
    );

    public TaskRouter(List<AiProviderAdapter> adapterList) {
        this.adapters = adapterList.stream()
                .collect(Collectors.toMap(AiProviderAdapter::getType, Function.identity(), (a, b) -> a));
    }

    public AiProviderAdapter route(AiTaskType taskType) {
        List<AiProviderType> priorityChain = getPriorityChain(taskType);

        for (AiProviderType providerType : priorityChain) {
            if (adapters.containsKey(providerType)) {
                log.debug("Routing task type {} to provider {}", taskType, providerType);
                return adapters.get(providerType);
            }
        }

        // Fail fast if no compatible provider in the failover chain is registered
        log.error("No compatible AI Provider Adapter available for task type: {}. Available registered adapters: {}",
                taskType, adapters.keySet());

        throw new IllegalStateException("No compatible AI provider available for task type: " + taskType +
                ". Available registered providers: " + adapters.keySet());
    }

    private List<AiProviderType> getPriorityChain(AiTaskType taskType) {
        if (taskType == null) return AGENT_TOOL_FAILOVER;
        return switch (taskType) {
            case AGENT_TOOL_CALLING -> AGENT_TOOL_FAILOVER;
            case RAG_QA -> RAG_QA_FAILOVER;
            case DOCUMENT_DRAFTING, SUMMARIZATION -> SUMMARIZATION_FAILOVER;
            case EMBEDDINGS -> EMBEDDINGS_FAILOVER;
        };
    }
}
