package com.nexusos.api.ai.agent.tool;

public class ToolResult {
    private boolean success;
    private String summary;
    private Object data;
    private String errorMessage;

    public ToolResult() {
    }

    public ToolResult(boolean success, String summary, Object data, String errorMessage) {
        this.success = success;
        this.summary = summary;
        this.data = data;
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean success;
        private String summary;
        private Object data;
        private String errorMessage;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder data(Object data) {
            this.data = data;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public ToolResult build() {
            return new ToolResult(success, summary, data, errorMessage);
        }
    }
}
