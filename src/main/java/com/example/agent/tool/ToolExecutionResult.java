package com.example.agent.tool;

/**
 * 角色定位：工具执行结果快照 (Execution Result Snapshot)
 * 链路定位：AgentTool.execute() 产出 -> ToolExecutor 包装 -> AgentLoop 记录为 Observation。
 * 业务意图：标准化统一所有工具的返回值。不管是成功的数据，还是失败的异常栈，都打包成文本喂回给大模型。
 */
public record ToolExecutionResult(
        boolean success,
        String toolName,
        // 工具执行成功时返回的内容实体
        String result,
        // 工具执行失败时的错误码
        String errorCode,
        // 工具执行失败时的错误详情（非常重要：大模型会阅读这个 errorMessage，并尝试在下一步修正参数重试）
        String errorMessage,
        // 执行耗时监控
        long durationMs
) {
    // 静态工厂方法：构建成功的执行结果
    public static ToolExecutionResult success(String toolName, String result, long durationMs) {
        return new ToolExecutionResult(true, toolName, result, null, null, durationMs);
    }

    // 静态工厂方法：构建失败的执行结果
    public static ToolExecutionResult failure(String toolName, String errorCode, String errorMessage, long durationMs) {
        return new ToolExecutionResult(false, toolName, null, errorCode, errorMessage, durationMs);
    }
}