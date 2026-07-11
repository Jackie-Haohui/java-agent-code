package com.example.agent.llm;

/**
 * 角色定位：大模型交互网关接口 (LLM Port)
 * 链路定位：被 AgentLoop 强依赖，用于触发大模型的思考。
 * 业务意图：定义了 Agent 系统“调用大脑”的标准契约。它体现了依赖倒置原则（DIP），
 * 使得核心大脑不依赖于具体的大模型 API（如 OpenAI / DashScope / 千问等）。
 */
public interface LlmClient {

    /**
     * 触发大模型进行一次文本补全/对话生成
     *
     * @param prompt 经过 AgentPromptBuilder 组装好的、包含上下文与工具清单的终极提示词
     * @return 统一格式的大模型响应结果
     */
    LlmResponse complete(String prompt);
}