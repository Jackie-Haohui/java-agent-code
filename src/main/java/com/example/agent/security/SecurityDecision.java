package com.example.agent.security;

/**
 * 角色定位：安全校验结果载体 (Security Decision Payload)
 * 链路定位：被所有安全服务（如 PromptInjectionGuard, ToolPermissionService）返回给 AgentLoop。
 * 业务意图：统一标准化的放行/拦截契约。不直接抛出异常，而是让外层框架决定是直接中断，还是将拒绝原因喂给大模型让其自我纠正。
 */
public record SecurityDecision(
        // 是否允许通行
        boolean allowed,
        // 如果被拒绝，具体的拒绝原因
        String reason
) {
    // 静态工厂方法：构建放行结果
    public static SecurityDecision allow() {
        return new SecurityDecision(true, "allowed");
    }

    // 静态工厂方法：构建拦截结果，并附带明确的拦截原因
    public static SecurityDecision deny(String reason) {
        return new SecurityDecision(false, reason);
    }
}