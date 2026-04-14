package com.nexusos.api.ai.agent.dto;

import java.util.UUID;

public class AgentResponse {
    private String textResponse;
    private boolean requiresConfirmation;
    private UUID pendingActivityId;
    private Object context;

    public AgentResponse() {
    }

    public AgentResponse(String textResponse, boolean requiresConfirmation, UUID pendingActivityId, Object context) {
        this.textResponse = textResponse;
        this.requiresConfirmation = requiresConfirmation;
        this.pendingActivityId = pendingActivityId;
        this.context = context;
    }

    public String getTextResponse() {
        return textResponse;
    }

    public void setTextResponse(String textResponse) {
        this.textResponse = textResponse;
    }

    public boolean isRequiresConfirmation() {
        return requiresConfirmation;
    }

    public void setRequiresConfirmation(boolean requiresConfirmation) {
        this.requiresConfirmation = requiresConfirmation;
    }

    public UUID getPendingActivityId() {
        return pendingActivityId;
    }

    public void setPendingActivityId(UUID pendingActivityId) {
        this.pendingActivityId = pendingActivityId;
    }

    public Object getContext() {
        return context;
    }

    public void setContext(Object context) {
        this.context = context;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String textResponse;
        private boolean requiresConfirmation;
        private UUID pendingActivityId;
        private Object context;

        public Builder textResponse(String textResponse) {
            this.textResponse = textResponse;
            return this;
        }

        public Builder requiresConfirmation(boolean requiresConfirmation) {
            this.requiresConfirmation = requiresConfirmation;
            return this;
        }

        public Builder pendingActivityId(UUID pendingActivityId) {
            this.pendingActivityId = pendingActivityId;
            return this;
        }

        public Builder context(Object context) {
            this.context = context;
            return this;
        }

        public AgentResponse build() {
            return new AgentResponse(textResponse, requiresConfirmation, pendingActivityId, context);
        }
    }
}
