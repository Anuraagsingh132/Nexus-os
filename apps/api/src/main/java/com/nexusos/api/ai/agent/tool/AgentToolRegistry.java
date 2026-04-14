package com.nexusos.api.ai.agent.tool;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AgentToolRegistry {
    private final Map<String, AgentTool> tools;

    public AgentToolRegistry(List<AgentTool> toolList) {
        this.tools = toolList.stream().collect(Collectors.toMap(AgentTool::getName, Function.identity()));
    }

    public Optional<AgentTool> getTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public List<AgentTool> getAllTools() {
        return List.copyOf(tools.values());
    }
}
