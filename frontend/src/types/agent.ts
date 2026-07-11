export interface AgentRunRequest {
  userId: string
  goal: string
  input?: Record<string, unknown>
  conversationId?: string
  workspacePath?: string
}

export interface ToolCallDto {
  toolName: string
  arguments: string
  result: string
  success: boolean
}

export interface AgentRunResponse {
  sessionId: string
  status: string
  finalAnswer: string | null
  toolCalls: ToolCallDto[]
  pendingApprovalId?: string
  pendingToolName?: string
  pendingToolArguments?: string
  conversationId?: string
}

export interface HumanApprovalRequest {
  approvalId: string
  approved: boolean
  reason?: string
}

export interface AgentSession {
  id: string
  userId: string
  goal: string
  status: string
  currentStep: number
  maxSteps: number
  summary: string | null
  createdAt: string
  updatedAt: string
}

export interface AgentStepLog {
  id: string
  sessionId: string
  stepIndex: number
  decisionType: string
  modelOutput: string | null
  toolName: string | null
  toolArguments: string | null
  toolResult: string | null
  errorMessage: string | null
  createdAt: string
}

export interface Conversation {
  id: string
  userId: string
  title: string
  workspacePath: string | null
  status: string
  createdAt: string
  updatedAt: string
}

export interface ConversationMessage {
  id: string
  conversationId: string
  sessionId: string | null
  role: 'USER' | 'AGENT' | 'SYSTEM'
  content: string
  toolCalls: ToolCallDto[]
  createdAt: string
}

export interface ChatRequest {
  userId: string
  message: string
  input?: Record<string, unknown>
}

export interface CreateConversationRequest {
  userId: string
  title?: string
  workspacePath?: string
}

export interface UpdateConversationRequest {
  title?: string
  workspacePath?: string
}

export interface WorkspaceValidationResult {
  valid: boolean
  message: string
  path?: string
  projectType?: string
}

export interface EvaluationStats {
  totalSessions: number
  successRate: number
  avgSteps: number
  avgLatencyMs: number
}
