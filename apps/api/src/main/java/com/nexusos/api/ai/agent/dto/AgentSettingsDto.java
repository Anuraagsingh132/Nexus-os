package com.nexusos.api.ai.agent.dto;

import java.util.Map;

public class AgentSettingsDto {
    private Boolean agentEnabled;
    private String agentMode;
    private String activeProvider;
    private Map<String, Object> providerConfigs;

    public AgentSettingsDto() {
    }

    public AgentSettingsDto(Boolean agentEnabled, String agentMode, String activeProvider, Map<String, Object> providerConfigs) {
        this.agentEnabled = agentEnabled;
        this.agentMode = agentMode;
        this.activeProvider = activeProvider;
        this.providerConfigs = providerConfigs;
    }

    public Boolean getAgentEnabled() {
        return agentEnabled;
    }

    public void setAgentEnabled(Boolean agentEnabled) {
        this.agentEnabled = agentEnabled;
    }

    public String getAgentMode() {
        return agentMode;
    }

    public void setAgentMode(String agentMode) {
        this.agentMode = agentMode;
    }

    public String getActiveProvider() {
        return activeProvider;
    }

    public void setActiveProvider(String activeProvider) {
        this.activeProvider = activeProvider;
    }

    public Map<String, Object> getProviderConfigs() {
        return providerConfigs;
    }

    public void setProviderConfigs(Map<String, Object> providerConfigs) {
        this.providerConfigs = providerConfigs;
    }
}
