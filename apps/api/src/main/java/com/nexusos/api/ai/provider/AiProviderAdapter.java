package com.nexusos.api.ai.provider;

public interface AiProviderAdapter {
    AiProviderType getType();
    
    boolean isAvailable();
    
    String generateText(String prompt, String systemPrompt);
    
    String generateToolCall(String prompt, String systemPrompt, String toolsJsonSchema);
}
