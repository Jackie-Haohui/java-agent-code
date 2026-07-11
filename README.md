java-agent-code
├── pom.xml
├── docker-compose.yml
├── README.md
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── example
│   │   │           └── agent
│   │   │               ├── JavaAgentCodeApplication.java
│   │   │               ├── api
│   │   │               │   ├── AgentController.java
│   │   │               │   ├── DocumentController.java
│   │   │               │   └── dto
│   │   │               │       ├── AgentRunRequest.java
│   │   │               │       ├── AgentRunResponse.java
│   │   │               │       ├── ToolCallDto.java
│   │   │               │       ├── DocumentUploadRequest.java
│   │   │               │       └── HumanApprovalRequest.java
│   │   │               ├── agent
│   │   │               │   ├── AgentLoop.java
│   │   │               │   ├── AgentDecision.java
│   │   │               │   ├── AgentDecisionType.java
│   │   │               │   ├── AgentRuntimeContext.java
│   │   │               │   ├── AgentStepResult.java
│   │   │               │   ├── AgentTerminationPolicy.java
│   │   │               │   └── AgentPromptBuilder.java
│   │   │               ├── workflow
│   │   │               │   ├── WorkflowEngine.java
│   │   │               │   ├── WorkflowType.java
│   │   │               │   ├── PlannerService.java
│   │   │               │   ├── TaskPlan.java
│   │   │               │   └── TaskStep.java
│   │   │               ├── tool
│   │   │               │   ├── AgentTool.java
│   │   │               │   ├── ToolDefinition.java
│   │   │               │   ├── ToolParameterSchema.java
│   │   │               │   ├── ToolRegistry.java
│   │   │               │   ├── ToolExecutor.java
│   │   │               │   ├── ToolExecutionResult.java
│   │   │               │   ├── tools
│   │   │               │   │   ├── CodeSearchTool.java
│   │   │               │   │   ├── WebSearchMockTool.java
│   │   │               │   │   ├── FileReadTool.java
│   │   │               │   │   └── ShellCommandTool.java
│   │   │               ├── rag
│   │   │               │   ├── EmbeddingService.java
│   │   │               │   ├── MilvusVectorStoreService.java
│   │   │               │   ├── RagService.java
│   │   │               │   ├── RetrievedChunk.java
│   │   │               │   └── DocumentIngestionService.java
│   │   │               ├── memory
│   │   │               │   ├── MemoryService.java
│   │   │               │   ├── MemoryType.java
│   │   │               │   ├── MemoryRecord.java
│   │   │               │   └── MemoryRepository.java
│   │   │               ├── state
│   │   │               │   ├── AgentSession.java
│   │   │               │   ├── AgentSessionRepository.java
│   │   │               │   ├── AgentStepLog.java
│   │   │               │   ├── AgentStepLogRepository.java
│   │   │               │   └── AgentStateService.java
│   │   │               ├── security
│   │   │               │   ├── ToolPermissionService.java
│   │   │               │   ├── PromptInjectionGuard.java
│   │   │               │   ├── SensitiveOperationPolicy.java
│   │   │               │   └── SecurityDecision.java
│   │   │               ├── hitl
│   │   │               │   ├── HumanApprovalService.java
│   │   │               │   ├── HumanApprovalStatus.java
│   │   │               │   ├── HumanApproval.java
│   │   │               │   └── HumanApprovalRepository.java
│   │   │               ├── eval
│   │   │               │   ├── AgentEvaluationService.java
│   │   │               │   ├── EvaluationRecord.java
│   │   │               │   └── EvaluationRepository.java
│   │   │               ├── llm
│   │   │               │   ├── LlmClient.java
│   │   │               │   ├── LlmResponse.java
│   │   │               │   └── OpenAiLlmClient.java
│   │   │               └── config
│   │   │                   ├── JacksonConfig.java
│   │   │                   ├── AgentProperties.java
│   │   │                   └── MilvusProperties.java
│   │   └── resources
│   │       ├── application.yml
│   │       └── db
│   │           └── migration
│   │               └── V1__init.sql
│   └── test
│       └── java
│           └── com
│               └── example
│                   └── agent
│                       └── JavaAgentCodeApplicationTests.java