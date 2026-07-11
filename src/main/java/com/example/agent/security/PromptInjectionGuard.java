package com.example.agent.security;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 角色定位：提示词防火墙 (Prompt Injection Firewall)
 * 链路定位：AgentLoop.run() 的第一行逻辑。在请求接触到大模型之前进行前置清洗。
 * 业务意图：防止恶意用户通过特定的“越狱 (Jailbreak)”指令篡改 Agent 的核心人设或套取系统机密。
 */
@Component
public class PromptInjectionGuard {

    // 【安全基线】：黑名单机制。包含了业界常见的用于攻击 LLM 的诱导性关键词。
    // 架构进阶方向：未来这里可以接入外部专门的 LLM 安全网关服务，或者使用小模型进行基于语义的恶意意图分类。
    private static final List<String> DANGEROUS_PATTERNS = List.of(
            "忽略之前的所有指令",
            "ignore previous instructions",
            "泄露系统提示词",
            "show system prompt",
            "disable safety",
            "绕过权限"
    );

    /**
     * 核心校验：审查用户的目标输入
     */
    public SecurityDecision inspectUserInput(String input) {
        // 1. 基础校验：防空指针与空洞任务
        if (input == null || input.isBlank()) {
            return SecurityDecision.deny("输入不能为空");
        }

        // 2. 统一转小写，增加匹配的鲁棒性
        String lower = input.toLowerCase();

        // 3. 遍历黑名单模式，如果命中任何一个恶意词汇，立即阻断
        for (String pattern : DANGEROUS_PATTERNS) {
            if (lower.contains(pattern.toLowerCase())) {
                return SecurityDecision.deny("疑似 Prompt Injection：" + pattern);
            }
        }

        // 4. 审查通过，放行交给后续的 AgentLoop 处理
        return SecurityDecision.allow();
    }
}