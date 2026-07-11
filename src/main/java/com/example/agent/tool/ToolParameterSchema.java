package com.example.agent.tool;

import java.util.Map;

/**
 * 角色定位：工具参数模型 (JSON Schema Definition)
 * 链路定位：被 ToolDefinition 包含 -> 被 AgentPromptBuilder 渲染给大模型。
 * 业务意图：告诉大模型“如果要用这个工具，你需要按什么格式传参”。本质上是 OpenAI Function Calling 规范的 Java 映射。
 */
public record ToolParameterSchema(
        // 参数总类型，通常固定为 "object"
        String type,
        // 具体的参数字段定义，Key 是字段名，Value 是该字段的描述（如 type, description）
        Map<String, Object> properties,
        // 必填参数的字段名数组，大模型在生成 JSON 时会尽量遵守这个约定
        String[] required
) {
}