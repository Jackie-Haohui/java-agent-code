package com.example.agent.tool;

import java.util.Map;

public interface AgentTool {

    ToolDefinition definition();

    ToolExecutionResult execute(Map<String, Object> arguments);
}