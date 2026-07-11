package com.example.agent.agent;

import com.example.agent.state.AgentSession;
import org.springframework.stereotype.Component;

/**
 * 角色定位：终止策略服务 (Termination Guard)
 * 链路定位：作用于 AgentLoop 的 while 循环条件判断。
 * 业务意图：系统的防失控安全阀，防止死循环。
 */
@Component
public class AgentTerminationPolicy {

    /**
     * 判断当前会话是否应该强制终止
     */
    public boolean shouldTerminate(AgentSession session) {
        // 如果状态机已经是完成状态，正常退出循环
        if ("COMPLETED".equals(session.getStatus())) {
            return true;
        }
        // 如果遇到了无法恢复的系统错误，异常退出循环
        if ("FAILED".equals(session.getStatus())) {
            return true;
        }
        // 熔断机制：如果 Agent 尝试思考/执行的步数超过了系统配置的最大允许步数（如 12 步），强制截断，防止无限消耗 Token
        return session.getCurrentStep() >= session.getMaxSteps();
    }
}