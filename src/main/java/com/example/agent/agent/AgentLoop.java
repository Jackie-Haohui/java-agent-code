package com.example.agent.agent;

import com.example.agent.api.dto.AgentRunRequest;
import com.example.agent.api.dto.AgentRunResponse;
import com.example.agent.api.dto.ToolCallDto;
import com.example.agent.config.AgentProperties;
import com.example.agent.conversation.ConversationService;
import com.example.agent.eval.AgentEvaluationService;
import com.example.agent.hitl.HumanApproval;
import com.example.agent.hitl.HumanApprovalRepository;
import com.example.agent.hitl.HumanApprovalService;
import com.example.agent.hitl.HumanApprovalStatus;
import com.example.agent.llm.LlmClient;
import com.example.agent.llm.LlmResponse;
import com.example.agent.memory.MemoryService;
import com.example.agent.rag.RagService;
import com.example.agent.security.PromptInjectionGuard;
import com.example.agent.security.SecurityDecision;
import com.example.agent.security.ToolPermissionService;
import com.example.agent.state.AgentSession;
import com.example.agent.state.AgentSessionRepository;
import com.example.agent.state.AgentStateService;
import com.example.agent.state.AgentStepLog;
import com.example.agent.state.AgentStepLogRepository;
import com.example.agent.tool.ToolExecutionResult;
import com.example.agent.tool.ToolExecutor;
import com.example.agent.tool.ToolRegistry;
import com.example.agent.tool.WorkspaceContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 角色定位: 系统核心大脑 (The ReAct Engine)
 * 业务意图: 统筹各个子模块, 维系 Agent 思考-行动-观察的主循环.
 */
@Service
public class AgentLoop {

    private final AgentProperties agentProperties;
    private final AgentStateService agentStateService;
    private final MemoryService memoryService;
    private final RagService ragService;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final ToolPermissionService toolPermissionService;
    private final HumanApprovalService humanApprovalService;
    private final PromptInjectionGuard promptInjectionGuard;
    private final AgentPromptBuilder promptBuilder;
    private final AgentTerminationPolicy terminationPolicy;
    private final LlmClient llmClient;
    private final AgentEvaluationService evaluationService;
    private final AgentSessionRepository sessionRepository;
    private final HumanApprovalRepository approvalRepository;
    private final AgentStepLogRepository stepLogRepository;
    private final WorkspaceContext workspaceContext;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(AgentLoop.class);

    public AgentLoop(
            AgentProperties agentProperties,
            AgentStateService agentStateService,
            MemoryService memoryService,
            RagService ragService,
            ToolRegistry toolRegistry,
            ToolExecutor toolExecutor,
            ToolPermissionService toolPermissionService,
            HumanApprovalService humanApprovalService,
            PromptInjectionGuard promptInjectionGuard,
            AgentPromptBuilder promptBuilder,
            AgentTerminationPolicy terminationPolicy,
            LlmClient llmClient,
            AgentEvaluationService evaluationService,
            AgentSessionRepository sessionRepository,
            HumanApprovalRepository approvalRepository,
            AgentStepLogRepository stepLogRepository,
            WorkspaceContext workspaceContext,
            ConversationService conversationService,
            ObjectMapper objectMapper
    ) {
        this.agentProperties = agentProperties;
        this.agentStateService = agentStateService;
        this.memoryService = memoryService;
        this.ragService = ragService;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.toolPermissionService = toolPermissionService;
        this.humanApprovalService = humanApprovalService;
        this.promptInjectionGuard = promptInjectionGuard;
        this.promptBuilder = promptBuilder;
        this.terminationPolicy = terminationPolicy;
        this.llmClient = llmClient;
        this.evaluationService = evaluationService;
        this.sessionRepository = sessionRepository;
        this.approvalRepository = approvalRepository;
        this.stepLogRepository = stepLogRepository;
        this.workspaceContext = workspaceContext;
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
    }

    /**
     * Agent 的核心业务执行流
     */
    public AgentRunResponse run(AgentRunRequest request) {
        long startTime = System.currentTimeMillis();

        SecurityDecision securityDecision = promptInjectionGuard.inspectUserInput(request.getGoal());
        if (!securityDecision.allowed()) {
            throw new IllegalArgumentException(securityDecision.reason());
        }

        String wsPath = request.getWorkspacePath();
        if (wsPath == null || wsPath.isBlank()) {
            wsPath = agentProperties.getWorkspacePath();
        }
        workspaceContext.set(Path.of(wsPath));

        try {
            return runInternal(request, startTime, wsPath);
        } finally {
            workspaceContext.clear();
        }
    }

    private AgentRunResponse runInternal(AgentRunRequest request, long startTime, String wsPath) {

        AgentSession session = agentStateService.createSession(
                request.getUserId(),
                request.getGoal(),
                agentProperties.getMaxSteps(),
                request.getConversationId(),
                wsPath
        );

        List<ToolCallDto> toolCalls = new ArrayList<>();
        String finalAnswer = null;

        // 硬熔断：追踪同一工具+参数的连续调用次数（成功或失败都算，防死循环）
        String lastToolName = null;
        String lastToolArgs = null;
        int consecutiveSameCalls = 0;

        // 立即保存用户原始问题到长期记忆，确保后续对话可以追溯
        memoryService.saveUserMessage(session.getUserId(), request.getGoal());

        AgentRuntimeContext context = new AgentRuntimeContext();
        context.setSession(session);
        context.setMemories(memoryService.findRelevantMemories(request.getUserId(), request.getGoal()));
        context.setConversationId(request.getConversationId());
        context.setWorkspacePath(wsPath);
        context.setAvailableTools(toolRegistry.listDefinitions());
        context.setRetrievedChunks(List.of()); // 不自动预填 RAG，由大模型自行判断是否需要检索
        context.setInput(request.getInput() != null ? request.getInput() : Map.of());

        // 告知大模型知识库里有哪些文档，不用语义搜索就能回答"有哪些文档"这类问题
        List<Map<String, Object>> docs = ragService.listDocuments();
        if (!docs.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (var doc : docs) {
                sb.append("  - ").append(doc.get("title")).append(" (").append(doc.get("chunks")).append(" 个切片)\n");
            }
            context.setKnowledgeBaseSummary(sb.toString());
        }

        log.info("Agent 启动 session={} goal={} maxSteps={}", session.getId(), request.getGoal(), session.getMaxSteps());

        while (!terminationPolicy.shouldTerminate(session)) {
            int remaining = session.getMaxSteps() - session.getCurrentStep();
            if (remaining <= 3) {
                context.addObservation("【系统警告】剩余步数: " + remaining + "，必须尽快输出 FINAL_ANSWER 结束任务。");
            }

            if (context.isStuck(3)) {
                context.addObservation("【系统警告】你已连续重复相同操作多次，请换思路（REPLAN）或基于已有信息输出 FINAL_ANSWER。");
                context.trackDecision(null);
            }

            String prompt = promptBuilder.build(context);
            int stepNum = session.getCurrentStep() + 1;
            log.info("═══ Step {}/{}: 发送 Prompt ({} chars) ═══", stepNum, session.getMaxSteps(), prompt.length());
            LlmResponse response = llmClient.complete(prompt);
            AgentDecision decision = parseDecision(response.content());

            String toolInfo = decision.getToolName() != null
                    ? decision.getToolName() + " | args=" + toJson(decision.getToolArguments())
                    : "N/A";
            log.info("═══ Step {}/{}: 决策={} | 工具={} | 内容={} ═══",
                    stepNum, session.getMaxSteps(), decision.getType(), toolInfo,
                    decision.getContent() != null ? decision.getContent().substring(0, Math.min(decision.getContent().length(), 150)) : "");

            if (decision.getType() == AgentDecisionType.FINAL_ANSWER) {
                context.trackDecision(null);
                finalAnswer = decision.getContent();
                session.markCompleted();
                agentStateService.saveSession(session);
                agentStateService.recordStep(session, decision, response.content(), null);
                memoryService.extractAndSaveMemories(session.getUserId(), session.getGoal(), finalAnswer);
                log.info("Agent session={} 完成, 最终答案长度={}", session.getId(), finalAnswer.length());
                break;
            }

            if (decision.getType() == AgentDecisionType.RAG_SEARCH) {
                context.trackDecision("RAG");
                context.setRetrievedChunks(ragService.search(decision.getContent(), agentProperties.getRagTopK()));
                agentStateService.recordStep(session, decision, response.content(), null);
                session.incrementStep();
                agentStateService.saveSession(session);
                continue;
            }

            if (decision.getType() == AgentDecisionType.TOOL_CALL) {
                context.trackDecision("TOOL:" + decision.getToolName());
                SecurityDecision toolSecurity = toolPermissionService.canCall(
                        decision.getToolName(),
                        decision.getToolArguments()
                );

                if (!toolSecurity.allowed()) {
                    context.addObservation("工具调用被安全策略拒绝: " + toolSecurity.reason());
                    agentStateService.recordStep(session, decision, response.content(), toolSecurity.reason());
                    session.incrementStep();
                    agentStateService.saveSession(session);
                    continue;
                }

                if (toolPermissionService.requiresHumanApproval(session.getId(), decision.getToolName())) {
                    HumanApproval approval = humanApprovalService.createApproval(
                            session.getId(),
                            decision.getToolName(),
                            toJson(decision.getToolArguments()),
                            "敏感工具调用需要人工审批"
                    );
                    session.markWaitingApproval();
                    agentStateService.saveSession(session);
                    agentStateService.recordStep(session, decision, response.content(), null);
                    AgentRunResponse hitlResp = new AgentRunResponse(
                            session.getId(), session.getStatus(), null, toolCalls,
                            approval.getId(), decision.getToolName(), toJson(decision.getToolArguments()));
                    hitlResp.setConversationId(request.getConversationId());
                    return hitlResp;
                }

                ToolExecutionResult result = toolExecutor.execute(
                        decision.getToolName(),
                        decision.getToolArguments()
                );

                String resultSummary = result.success()
                        ? (result.result() != null ? result.result().substring(0, Math.min(result.result().length(), 200)) : "null")
                        : "失败: " + result.errorMessage();
                log.info("═══ Step {}/{}: 工具执行 {} → {} | {} ═══",
                        stepNum, session.getMaxSteps(), decision.getToolName(),
                        result.success() ? "✓ 成功" : "✗ 失败", resultSummary);

                toolCalls.add(new ToolCallDto(
                        decision.getToolName(),
                        toJson(decision.getToolArguments()),
                        result.result(),
                        result.success()
                ));

                // 硬熔断：同一工具+参数连续重复调用（失败≥2次，成功≥4次）
                // 每个新问题=新Session，计数器自动归零；同一问题内允许读同一文件最多3次
                String currentArgs = toJson(decision.getToolArguments());
                if (decision.getToolName().equals(lastToolName) && currentArgs.equals(lastToolArgs)) {
                    consecutiveSameCalls++;
                } else {
                    lastToolName = decision.getToolName();
                    lastToolArgs = currentArgs;
                    consecutiveSameCalls = 1;
                }

                if (!result.success() && consecutiveSameCalls >= 2) {
                    finalAnswer = "工具 " + decision.getToolName() + " 同一操作连续失败 " + consecutiveSameCalls
                            + " 次，系统强制终止。错误: " + (result.errorMessage() != null ? result.errorMessage() : result.result());
                    session.markFailed();
                    agentStateService.saveSession(session);
                    agentStateService.recordStep(session, decision, response.content(), result.result());
                    break;
                }

                if (result.success() && consecutiveSameCalls >= 4) {
                    finalAnswer = "工具 " + decision.getToolName() + " 同一操作已连续成功 " + consecutiveSameCalls
                            + " 次，系统判定陷入循环，强制终止。不要重复调用相同的工具和参数，请换思路（REPLAN）或基于已有信息输出 FINAL_ANSWER。";
                    session.markFailed();
                    agentStateService.saveSession(session);
                    agentStateService.recordStep(session, decision, response.content(), result.result());
                    break;
                }

                context.addObservation("工具 " + decision.getToolName() + " 返回: " + truncateObservation(result.result()));
                agentStateService.recordStep(session, decision, response.content(), result.result());
                session.incrementStep();
                agentStateService.saveSession(session);
                continue;
            }

            if (decision.getType() == AgentDecisionType.NEED_HUMAN_APPROVAL) {
                String toolName = decision.getToolName() != null ? decision.getToolName() : "sensitive_operation";
                String toolArgs = toJson(decision.getToolArguments());
                HumanApproval approval = humanApprovalService.createApproval(
                        session.getId(), toolName, toolArgs,
                        decision.getContent() != null ? decision.getContent() : "需要人工审批");
                session.markWaitingApproval();
                agentStateService.saveSession(session);
                agentStateService.recordStep(session, decision, response.content(), null);
                AgentRunResponse needHApprovalResp = new AgentRunResponse(
                        session.getId(), session.getStatus(), null, toolCalls,
                        approval.getId(), toolName, toolArgs);
                needHApprovalResp.setConversationId(request.getConversationId());
                return needHApprovalResp;
            }

            if (decision.getType() == AgentDecisionType.REPLAN) {
                context.trackDecision("REPLAN");
                context.addObservation("模型请求重新规划: " + decision.getContent());
                agentStateService.recordStep(session, decision, response.content(), null);
                session.incrementStep();
                agentStateService.saveSession(session);
                continue;
            }

            session.markFailed();
            agentStateService.saveSession(session);
            finalAnswer = "Agent 无法继续执行: " + decision.getContent();
            break;
        }

        if (finalAnswer == null && session.getCurrentStep() >= session.getMaxSteps()) {
            session.markFailed();
            agentStateService.saveSession(session);
            finalAnswer = "Agent 达到最大步骤限制, 任务未完成.";
        }

        log.info("Agent session={} 结束 status={} steps={}/{} latency={}ms",
                session.getId(), session.getStatus(), session.getCurrentStep(), session.getMaxSteps(),
                System.currentTimeMillis() - startTime);

        boolean success = "COMPLETED".equals(session.getStatus());
        long latency = System.currentTimeMillis() - startTime;
        long successCalls = toolCalls.stream().filter(ToolCallDto::isSuccess).count();
        double toolCallAccuracy = toolCalls.isEmpty() ? (success ? 1.0 : 0.0) : (double) successCalls / toolCalls.size();
        double stepReasonability = success ? Math.max(0.0, 1.0 - (double) session.getCurrentStep() / session.getMaxSteps()) : 0.0;
        try {
            evaluationService.recordEvaluation(
                    session.getId(),
                    success,
                    toolCallAccuracy,
                    stepReasonability,
                    latency,
                    session.getCurrentStep(),
                    success ? null : finalAnswer
            );
        } catch (Exception e) {
            log.warn("评估记录保存失败 (session={}): {}", session.getId(), e.getMessage());
        }

        AgentRunResponse response = new AgentRunResponse(session.getId(), session.getStatus(), finalAnswer, toolCalls);
        response.setConversationId(request.getConversationId());

        if (request.getConversationId() != null && finalAnswer != null) {
            try {
                conversationService.addAgentMessage(request.getConversationId(), session.getId(), finalAnswer, toolCalls);
            } catch (Exception e) {
                log.error("Agent 消息保存失败 (conversation={}, session={}): {}", request.getConversationId(), session.getId(), e.getMessage());
            }
        }

        return response;
    }

    /**
     * 恢复被 HITL 挂起的 Agent 会话, 从审批点继续执行.
     * 在管理员审批完高危工具调用后调用.
     */
    public AgentRunResponse resume(UUID sessionId) {
        AgentSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        if (!"WAITING_APPROVAL".equals(session.getStatus())) {
            throw new IllegalStateException("Session is not waiting for approval: " + session.getStatus());
        }

        HumanApproval approval = approvalRepository
                .findTopBySessionIdOrderByCreatedAtDesc(sessionId)
                .orElseThrow(() -> new IllegalStateException("No pending approval found for session: " + sessionId));

        log.info("Agent 恢复 session={} 审批工具={} status={}", sessionId, approval.getToolName(), approval.getStatus());

        String wsPath = session.getWorkspacePath();
        if (wsPath == null || wsPath.isBlank()) {
            wsPath = agentProperties.getWorkspacePath();
        }
        workspaceContext.set(Path.of(wsPath));

        try {
            return resumeInternal(session, approval, sessionId);
        } finally {
            workspaceContext.clear();
        }
    }

    private AgentRunResponse resumeInternal(AgentSession session, HumanApproval approval, UUID sessionId) {

        long startTime = System.currentTimeMillis();

        session.incrementStep();
        agentStateService.saveSession(session);

        List<ToolCallDto> toolCalls = new ArrayList<>();
        String finalAnswer = null;

        // 硬熔断：追踪同一工具+参数的连续调用次数
        String lastToolName = null;
        String lastToolArgs = null;
        int consecutiveSameCalls = 0;

        AgentRuntimeContext context = new AgentRuntimeContext();
        context.setSession(session);
        context.setConversationId(session.getConversationId());
        context.setWorkspacePath(session.getWorkspacePath());
        context.setAvailableTools(toolRegistry.listDefinitions());
        context.setMemories(memoryService.findRelevantMemories(session.getUserId(), session.getGoal()));
        context.setRetrievedChunks(ragService.search(session.getGoal(), agentProperties.getRagTopK()));

        if (approval.getStatus() == HumanApprovalStatus.APPROVED) {
            context.addObservation("人工审批已通过工具 " + approval.getToolName() + " 的调用, 原因: " + approval.getReason());
            ToolExecutionResult result = toolExecutor.execute(
                    approval.getToolName(),
                    parseArguments(approval.getToolArguments())
            );
            log.info("Agent 审批工具执行: {} 成功={}", approval.getToolName(), result.success());
            toolCalls.add(new ToolCallDto(
                    approval.getToolName(),
                    approval.getToolArguments(),
                    result.result(),
                    result.success()
            ));
            context.addObservation("工具 " + approval.getToolName() + " 返回: " + truncateObservation(result.result()));
            toolPermissionService.markSessionToolApproved(sessionId, approval.getToolName());
        } else {
            context.addObservation("人工审批已拒绝工具 " + approval.getToolName() + " 的调用, 原因: " + approval.getReason());
        }

        // 恢复历史上下文：加载该 Session 之前所有步骤的工具调用和结果，让 LLM 知道之前做了什么
        List<AgentStepLog> historyLogs = stepLogRepository.findBySessionIdOrderByStepIndexAsc(sessionId);
        if (!historyLogs.isEmpty()) {
            context.addObservation("=== 以下是本次会话之前的执行记录（共 " + historyLogs.size() + " 步）===");
            for (AgentStepLog historyLog : historyLogs) {
                if (historyLog.getToolName() != null && !historyLog.getToolName().isBlank()) {
                    String toolResult = historyLog.getToolResult() != null ? truncateObservation(historyLog.getToolResult()) : "null";
                    context.addObservation("[第" + (historyLog.getStepIndex() + 1) + "步] 调用了工具 " + historyLog.getToolName()
                            + "，返回: " + toolResult);
                }
            }
            context.addObservation("=== 以上是历史执行记录，请基于这些已有结果继续推进任务，不要重复已经成功的操作 ===");
            log.info("Agent resume session={}: 加载了 {} 条历史步骤日志", sessionId, historyLogs.size());
        }

        while (!terminationPolicy.shouldTerminate(session)) {
            int remaining = session.getMaxSteps() - session.getCurrentStep();
            if (remaining <= 3) {
                context.addObservation("【系统警告】剩余步数: " + remaining + "，必须尽快输出 FINAL_ANSWER 结束任务。");
            }

            if (context.isStuck(3)) {
                context.addObservation("【系统警告】你已连续重复相同操作多次，请换思路（REPLAN）或基于已有信息输出 FINAL_ANSWER。");
                context.trackDecision(null);
            }

            String prompt = promptBuilder.build(context);
            int stepNum = session.getCurrentStep() + 1;
            log.info("═══ Step {}/{} [resume]: 发送 Prompt ({} chars) ═══", stepNum, session.getMaxSteps(), prompt.length());
            LlmResponse response = llmClient.complete(prompt);
            AgentDecision decision = parseDecision(response.content());

            String toolInfo = decision.getToolName() != null
                    ? decision.getToolName() + " | args=" + toJson(decision.getToolArguments())
                    : "N/A";
            log.info("═══ Step {}/{} [resume]: 决策={} | 工具={} | 内容={} ═══",
                    stepNum, session.getMaxSteps(), decision.getType(), toolInfo,
                    decision.getContent() != null ? decision.getContent().substring(0, Math.min(decision.getContent().length(), 150)) : "");

            if (decision.getType() == AgentDecisionType.FINAL_ANSWER) {
                context.trackDecision(null);
                finalAnswer = decision.getContent();
                session.markCompleted();
                agentStateService.saveSession(session);
                agentStateService.recordStep(session, decision, response.content(), null);
                memoryService.extractAndSaveMemories(session.getUserId(), session.getGoal(), finalAnswer);
                log.info("Agent resume session={} 完成", session.getId());
                break;
            }

            if (decision.getType() == AgentDecisionType.RAG_SEARCH) {
                context.trackDecision("RAG");
                context.setRetrievedChunks(ragService.search(decision.getContent(), agentProperties.getRagTopK()));
                agentStateService.recordStep(session, decision, response.content(), null);
                session.incrementStep();
                agentStateService.saveSession(session);
                continue;
            }

            if (decision.getType() == AgentDecisionType.TOOL_CALL) {
                context.trackDecision("TOOL:" + decision.getToolName());
                SecurityDecision toolSecurity = toolPermissionService.canCall(
                        decision.getToolName(),
                        decision.getToolArguments()
                );

                if (!toolSecurity.allowed()) {
                    context.addObservation("工具调用被安全策略拒绝: " + toolSecurity.reason());
                    agentStateService.recordStep(session, decision, response.content(), toolSecurity.reason());
                    session.incrementStep();
                    agentStateService.saveSession(session);
                    continue;
                }

                if (toolPermissionService.requiresHumanApproval(session.getId(), decision.getToolName())) {
                    HumanApproval newApproval = humanApprovalService.createApproval(
                            session.getId(),
                            decision.getToolName(),
                            toJson(decision.getToolArguments()),
                            "敏感工具调用需要人工审批"
                    );
                    session.markWaitingApproval();
                    agentStateService.saveSession(session);
                    agentStateService.recordStep(session, decision, response.content(), null);
                    AgentRunResponse resumeHitlResp = new AgentRunResponse(
                            session.getId(), session.getStatus(), null, toolCalls,
                            newApproval.getId(), decision.getToolName(), toJson(decision.getToolArguments()));
                    resumeHitlResp.setConversationId(session.getConversationId());
                    return resumeHitlResp;
                }

                ToolExecutionResult result = toolExecutor.execute(
                        decision.getToolName(),
                        decision.getToolArguments()
                );

                String resultSummary = result.success()
                        ? (result.result() != null ? result.result().substring(0, Math.min(result.result().length(), 200)) : "null")
                        : "失败: " + result.errorMessage();
                log.info("═══ Step {}/{}: 工具执行 {} → {} | {} ═══",
                        stepNum, session.getMaxSteps(), decision.getToolName(),
                        result.success() ? "✓ 成功" : "✗ 失败", resultSummary);

                toolCalls.add(new ToolCallDto(
                        decision.getToolName(),
                        toJson(decision.getToolArguments()),
                        result.result(),
                        result.success()
                ));

                // 硬熔断：同一工具+参数连续重复调用（失败≥2次，成功≥4次）
                String currentArgs2 = toJson(decision.getToolArguments());
                if (decision.getToolName().equals(lastToolName) && currentArgs2.equals(lastToolArgs)) {
                    consecutiveSameCalls++;
                } else {
                    lastToolName = decision.getToolName();
                    lastToolArgs = currentArgs2;
                    consecutiveSameCalls = 1;
                }

                if (!result.success() && consecutiveSameCalls >= 2) {
                    finalAnswer = "工具 " + decision.getToolName() + " 同一操作连续失败 " + consecutiveSameCalls
                            + " 次，系统强制终止。错误: " + (result.errorMessage() != null ? result.errorMessage() : result.result());
                    session.markFailed();
                    agentStateService.saveSession(session);
                    agentStateService.recordStep(session, decision, response.content(), result.result());
                    break;
                }

                if (result.success() && consecutiveSameCalls >= 4) {
                    finalAnswer = "工具 " + decision.getToolName() + " 同一操作已连续成功 " + consecutiveSameCalls
                            + " 次，系统判定陷入循环，强制终止。不要重复调用相同的工具和参数，请换思路（REPLAN）或基于已有信息输出 FINAL_ANSWER。";
                    session.markFailed();
                    agentStateService.saveSession(session);
                    agentStateService.recordStep(session, decision, response.content(), result.result());
                    break;
                }

                context.addObservation("工具 " + decision.getToolName() + " 返回: " + truncateObservation(result.result()));
                agentStateService.recordStep(session, decision, response.content(), result.result());
                session.incrementStep();
                agentStateService.saveSession(session);
                continue;
            }

            if (decision.getType() == AgentDecisionType.NEED_HUMAN_APPROVAL) {
                String toolName = decision.getToolName() != null ? decision.getToolName() : "sensitive_operation";
                String toolArgs = toJson(decision.getToolArguments());
                HumanApproval newApproval2 = humanApprovalService.createApproval(
                        session.getId(), toolName, toolArgs,
                        decision.getContent() != null ? decision.getContent() : "需要人工审批");
                session.markWaitingApproval();
                agentStateService.saveSession(session);
                agentStateService.recordStep(session, decision, response.content(), null);
                AgentRunResponse resumeNeedHResp = new AgentRunResponse(
                        session.getId(), session.getStatus(), null, toolCalls,
                        newApproval2.getId(), toolName, toolArgs);
                resumeNeedHResp.setConversationId(session.getConversationId());
                return resumeNeedHResp;
            }

            if (decision.getType() == AgentDecisionType.REPLAN) {
                context.trackDecision("REPLAN");
                context.addObservation("模型请求重新规划: " + decision.getContent());
                agentStateService.recordStep(session, decision, response.content(), null);
                session.incrementStep();
                agentStateService.saveSession(session);
                continue;
            }

            session.markFailed();
            agentStateService.saveSession(session);
            finalAnswer = "Agent 无法继续执行: " + decision.getContent();
            break;
        }

        if (finalAnswer == null && session.getCurrentStep() >= session.getMaxSteps()) {
            session.markFailed();
            agentStateService.saveSession(session);
            finalAnswer = "Agent 达到最大步骤限制, 任务未完成.";
        }

        log.info("Agent resume session={} 结束 status={} steps={}/{}",
                session.getId(), session.getStatus(), session.getCurrentStep(), session.getMaxSteps());

        boolean success = "COMPLETED".equals(session.getStatus());
        long latency = System.currentTimeMillis() - startTime;
        long successCalls = toolCalls.stream().filter(ToolCallDto::isSuccess).count();
        double toolCallAccuracy = toolCalls.isEmpty() ? (success ? 1.0 : 0.0) : (double) successCalls / toolCalls.size();
        double stepReasonability = success ? Math.max(0.0, 1.0 - (double) session.getCurrentStep() / session.getMaxSteps()) : 0.0;
        try {
            evaluationService.recordEvaluation(
                    session.getId(),
                    success,
                    toolCallAccuracy,
                    stepReasonability,
                    latency,
                    session.getCurrentStep(),
                    success ? null : finalAnswer
            );
        } catch (Exception e) {
            log.warn("评估记录保存失败 (session={}): {}", session.getId(), e.getMessage());
        }

        AgentRunResponse resumeResponse = new AgentRunResponse(session.getId(), session.getStatus(), finalAnswer, toolCalls);
        resumeResponse.setConversationId(session.getConversationId());

        if (session.getConversationId() != null && finalAnswer != null) {
            try {
                conversationService.addAgentMessage(session.getConversationId(), session.getId(), finalAnswer, toolCalls);
            } catch (Exception e) {
                log.error("Agent 消息保存失败 (conversation={}, session={}): {}", session.getConversationId(), session.getId(), e.getMessage());
            }
        }

        return resumeResponse;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArguments(String toolArguments) {
        try {
            return objectMapper.readValue(toolArguments, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * 截断工具输出，防止大文本撑爆 LLM 上下文窗口，导致注意力崩溃 + 死循环。
     */
    private String truncateObservation(String text) {
        if (text == null) return "null";
        if (text.length() <= 5000) return text;
        return text.substring(0, 5000);
    }

    private AgentDecision parseDecision(String content) {
        try {
            return objectMapper.readValue(content, AgentDecision.class);
        } catch (Exception e) {
            AgentDecision salvaged = trySalvageDecision(content);
            if (salvaged != null) {
                log.warn("Agent LLM 输出格式不标准但可修复: type={} toolName={}", salvaged.getType(), salvaged.getToolName());
                return salvaged;
            }
            return new AgentDecision(
                    AgentDecisionType.ERROR,
                    "模型输出不是合法 JSON: " + content,
                    null,
                    null
            );
        }
    }

    @SuppressWarnings("unchecked")
    private AgentDecision trySalvageDecision(String content) {
        try {
            Map<String, Object> map = objectMapper.readValue(content, Map.class);
            String rawType = map.get("type") instanceof String s ? s : null;
            if (rawType == null) return null;
            String text = map.get("content") instanceof String s ? s : null;
            String rawToolName = map.get("toolName") instanceof String s ? s : null;
            Map<String, Object> rawArgs = map.get("toolArguments") instanceof Map ? (Map<String, Object>) map.get("toolArguments") : null;

            for (AgentDecisionType type : AgentDecisionType.values()) {
                if (type.name().equalsIgnoreCase(rawType)) {
                    return new AgentDecision(type, text, rawToolName, rawArgs);
                }
            }

            if (rawToolName != null && toolRegistry.exists(rawToolName)) {
                return new AgentDecision(AgentDecisionType.TOOL_CALL, text, rawToolName, rawArgs);
            }

            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }
}
