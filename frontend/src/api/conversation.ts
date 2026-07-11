import { http } from './http'
import type {
  Conversation,
  ConversationMessage,
  CreateConversationRequest,
  UpdateConversationRequest,
  ChatRequest,
  WorkspaceValidationResult,
  AgentRunResponse,
  HumanApprovalRequest,
} from '@/types/agent'

export function createConversation(data: CreateConversationRequest) {
  return http.post<Conversation>('/api/conversations', data)
}

export function listConversations(userId: string) {
  return http.get<Conversation[]>('/api/conversations', { params: { userId } })
}

export function getConversation(conversationId: string) {
  return http.get<Conversation>(`/api/conversations/${conversationId}`)
}

export function getMessages(conversationId: string) {
  return http.get<ConversationMessage[]>(`/api/conversations/${conversationId}/messages`)
}

export function sendChat(conversationId: string, data: ChatRequest) {
  return http.post<AgentRunResponse>(`/api/conversations/${conversationId}/chat`, data)
}

export function updateConversation(conversationId: string, data: UpdateConversationRequest) {
  return http.put<Conversation>(`/api/conversations/${conversationId}`, data)
}

export function deleteConversation(conversationId: string) {
  return http.delete(`/api/conversations/${conversationId}`)
}

export function validateWorkspace(path: string) {
  return http.post<WorkspaceValidationResult>('/api/workspace/validate', { path })
}

export function resumeConversation(conversationId: string, data: HumanApprovalRequest) {
  return http.post<AgentRunResponse>(`/api/conversations/${conversationId}/resume`, data)
}

export function uploadWorkspace(formData: FormData) {
  return http.post('/api/workspace/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000,
  })
}

export function cloneRepository(url: string) {
  return http.post('/api/workspace/clone', { url })
}
