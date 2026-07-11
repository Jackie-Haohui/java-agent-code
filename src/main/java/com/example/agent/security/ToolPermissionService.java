package com.example.agent.security;

import com.example.agent.tool.ToolRegistry;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ToolPermissionService {

    private final ToolRegistry toolRegistry;
    private final SensitiveOperationPolicy sensitiveOperationPolicy;

    // 会话级别审批豁免：同一个会话内，同类敏感工具审批通过后，后续调用自动跳过 HITL
    private final Map<UUID, Set<String>> sessionApprovedTools = new ConcurrentHashMap<>();

    public ToolPermissionService(
            ToolRegistry toolRegistry,
            SensitiveOperationPolicy sensitiveOperationPolicy
    ) {
        this.toolRegistry = toolRegistry;
        this.sensitiveOperationPolicy = sensitiveOperationPolicy;
    }

    public SecurityDecision canCall(String toolName, Map<String, Object> arguments) {
        if (toolName == null || toolName.isBlank()) {
            return SecurityDecision.deny("工具名不能为空");
        }
        if (!toolRegistry.exists(toolName)) {
            return SecurityDecision.deny("工具不存在：" + toolName);
        }
        if (arguments == null) {
            return SecurityDecision.deny("工具参数不能为空");
        }
        return SecurityDecision.allow();
    }

    /**
     * 同一会话内，某类工具已审批通过不再重复弹窗
     */
    public boolean requiresHumanApproval(UUID sessionId, String toolName) {
        if (!sensitiveOperationPolicy.isSensitiveTool(toolName)) {
            return false;
        }
        Set<String> approved = sessionApprovedTools.get(sessionId);
        return approved == null || !approved.contains(toolName);
    }

    /**
     * 标记该会话中此类工具已获审批，后续调用跳过 HITL
     */
    public void markSessionToolApproved(UUID sessionId, String toolName) {
        sessionApprovedTools.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(toolName);
    }

    public void clearSessionApprovals(UUID sessionId) {
        sessionApprovedTools.remove(sessionId);
    }
}