package com.example.agent.llm;

public record LlmResponse(String content) {
    // 大模型生成的纯文本内容（通常是一段 JSON 格式的 AgentDecision 字符串）
}