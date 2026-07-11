package com.example.agent.security;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 角色定位：高危操作定义策略 (High-Risk Operation Policy)
 * 链路定位：被 ToolPermissionService 依赖，用于判别是否触发 HITL (人机协同)。
 * 业务意图：集中管理系统中的敏感工具名单。
 */
@Component
public class SensitiveOperationPolicy {

    // 【安全基线】：将具备破坏性、非幂等性或极高系统权限的工具纳入此处。
    // 目前纳入了 shell_command，未来如果新增 DB 修改工具 (如 execute_sql)，也必须加在这里。
    private static final Set<String> SENSITIVE_TOOLS = Set.of(
            "shell_command",
            "file_write",
            "file_edit",
            "git_operation"
    );

    /**
     * 判断传入的工具是否属于高危敏感工具
     */
    public boolean isSensitiveTool(String toolName) {
        return SENSITIVE_TOOLS.contains(toolName);
    }
}