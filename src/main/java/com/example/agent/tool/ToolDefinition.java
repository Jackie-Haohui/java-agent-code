package com.example.agent.tool;

/**
 * 角色定位：工具说明书 (Tool Meta Info)
 * 链路定位：向大模型注册能力的名片。
 * 业务意图：让大模型知道“我是谁，我能干什么，我有什么参数限制”。
 */
public record ToolDefinition(
        // 工具的唯一英文标识符（大模型输出的 decision.toolName 必须和这个严格匹配）
        String name,
        // 工具的自然语言描述。极其重要！大模型就是靠阅读这段描述来决定是否要在当前步骤使用该工具的。
        String description,
        // 该工具需要的参数格式
        ToolParameterSchema parameterSchema,
        // 敏感开关：如果为 true，当大模型决定调用它时，AgentLoop 会拦截并挂起任务，等待人工审批 (HITL)
        boolean sensitive
) {
}