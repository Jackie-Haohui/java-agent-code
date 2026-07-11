package com.example.agent.api.dto;

/**
 * 角色: 工具调用明细 (Tool Audit Trail)
 * 链路: ToolExecutor -> AgentStepLog -> AgentRunResponse
 * 作用: 作为数据传输对象，向前端或调用方展示单次工具交互的具体细节。
 */
public class ToolCallDto {

    private String toolName;
    private String arguments;
    private String result;
    private boolean success;

    public ToolCallDto(String toolName, String arguments, String result, boolean success) {
        this.toolName = toolName;
        this.arguments = arguments;
        this.result = result;
        this.success = success;
    }

    public String getToolName() { return toolName; }
    public String getArguments() { return arguments; }
    public String getResult() { return result; }
    public boolean isSuccess() { return success; }
}