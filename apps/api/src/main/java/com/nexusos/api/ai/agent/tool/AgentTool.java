package com.nexusos.api.ai.agent.tool;

import java.util.Map;
import java.util.UUID;

public interface AgentTool {
    String getName();
    String getDescription();
    Map<String, Object> getParameterSchema();
    boolean isHighImpact();
    ToolResult execute(UUID workspaceId, UUID requestingUserId, Map<String, Object> arguments);
}
