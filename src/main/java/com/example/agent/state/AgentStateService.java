package com.example.agent.state;

import com.example.agent.agent.AgentDecision;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AgentStateService {

    private final AgentSessionRepository sessionRepository;
    private final AgentStepLogRepository stepLogRepository;
    private final ObjectMapper objectMapper;

    public AgentStateService(
            AgentSessionRepository sessionRepository,
            AgentStepLogRepository stepLogRepository,
            ObjectMapper objectMapper
    ) {
        this.sessionRepository = sessionRepository;
        this.stepLogRepository = stepLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AgentSession createSession(String userId, String goal, int maxSteps) {
        return createSession(userId, goal, maxSteps, null, null);
    }

    @Transactional
    public AgentSession createSession(String userId, String goal, int maxSteps, UUID conversationId, String workspacePath) {
        return sessionRepository.save(AgentSession.create(userId, goal, maxSteps, conversationId, workspacePath));
    }

    @Transactional
    public void saveSession(AgentSession session) {
        sessionRepository.save(session);
    }

    @Transactional
    public void recordStep(
            AgentSession session,
            AgentDecision decision,
            String modelOutput,
            String resultOrError
    ) {
        String arguments = "";
        try {
            arguments = objectMapper.writeValueAsString(decision.getToolArguments());
        } catch (Exception ignored) {
        }

        AgentStepLog log = AgentStepLog.create(
                session.getId(),
                session.getCurrentStep(),
                decision.getType().name(),
                modelOutput,
                decision.getToolName(),
                arguments,
                resultOrError,
                null
        );

        stepLogRepository.save(log);
    }
}