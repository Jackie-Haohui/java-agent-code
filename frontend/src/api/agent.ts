import { http } from './http'
import type { AgentRunRequest, AgentRunResponse, AgentSession, AgentStepLog, HumanApprovalRequest, EvaluationStats } from '@/types/agent'

export function runAgent(data: AgentRunRequest) {
  return http.post<AgentRunResponse>('/api/agents/run', data)
}

export function submitApproval(data: HumanApprovalRequest) {
  return http.post<void>('/api/agents/approval', data)
}

export function listSessions(userId?: string) {
  return http.get<AgentSession[]>('/api/agents/sessions', { params: userId ? { userId } : undefined })
}

export function getSessionSteps(sessionId: string) {
  return http.get<AgentStepLog[]>(`/api/agents/sessions/${sessionId}/steps`)
}

export function getEvalStats(userId: string) {
  return http.get<EvaluationStats>('/api/evaluations/stats', { params: { userId } })
}
