package com.example.agent.tool;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 角色定位：工具安全执行沙箱 (Execution Sandbox)
 * 链路定位：被 AgentLoop 唯一调用，用于真正触发物理动作。
 * 业务意图：隔离底层异常。绝不允许某一个工具的崩溃（如空指针、网络超时）导致整个 Agent 主循环崩溃。
 */
@Service
public class ToolExecutor {

    private final ToolRegistry toolRegistry;

    public ToolExecutor(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /**
     * 执行拦截封装
     */
    public ToolExecutionResult execute(String toolName, Map<String, Object> arguments) {
        long start = System.currentTimeMillis();

        try {
            // 从注册中心拿到具体的工具
            AgentTool tool = toolRegistry.get(toolName);
            // 真正执行逻辑
            return tool.execute(arguments);
        } catch (Exception e) {
            // 【核心防御】：捕获所有运行时异常，并将其转化为 ToolExecutionResult.failure。
            // 这样 AgentLoop 不会崩溃，而且大模型会在下一步的 observation 中看到这个 e.getMessage()，从而知道自己传错参数了。
            return ToolExecutionResult.failure(
                    toolName,
                    "TOOL_EXECUTION_ERROR",
                    e.getMessage(),
                    System.currentTimeMillis() - start
            );
        }
    }
}