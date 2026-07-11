<template>
  <div class="chat-layout">
    <ConversationSidebar
      :conversations="conversations"
      :activeId="activeConversationId"
      @select="selectConversation"
      @new-conversation="showNewDialog = true"
      @delete="handleDeleteConversation"
    />

    <div class="chat-area" v-if="activeConversationId">
      <div class="message-list" ref="messageListRef">
        <div v-if="messages.length === 0 && !running" class="welcome-hint">
          <p>Start a conversation by sending a message below.</p>
          <p class="workspace-info" v-if="activeWorkspacePath">
            Workspace: {{ activeWorkspacePath }}
          </p>
        </div>

        <MessageBubble
          v-for="(msg, index) in messages"
          :key="msg.id"
          :message="msg"
          :can-regenerate="msg.role === 'AGENT' && index === lastAgentMessageIndex && !running && !pendingApproval"
          @copy="handleCopyMessage"
          @regenerate="handleRegenerate"
        />

        <div v-if="running" class="thinking-indicator">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>Agent is thinking...</span>
        </div>

        <ApprovalCard
          v-if="pendingApproval"
          :approval-id="pendingApproval.approvalId"
          :tool-name="pendingApproval.toolName"
          :tool-arguments="pendingApproval.toolArguments"
          @approve="handleApprove"
          @reject="handleReject"
        />
      </div>

      <ChatInput
        :disabled="running || !!pendingApproval"
        :workspace-path="activeWorkspacePath"
        @send="handleSend"
      />
    </div>

    <div class="chat-placeholder" v-else>
      <div class="placeholder-content">
        <h2>Java Agent Code</h2>
        <p>AI-powered code modification assistant</p>
        <el-button type="primary" @click="showNewDialog = true">New Conversation</el-button>
      </div>
    </div>

    <ProjectImportDialog
      :visible="showNewDialog"
      @confirm="handleCreateConversation"
      @cancel="showNewDialog = false"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import ConversationSidebar from '@/components/ConversationSidebar.vue'
import MessageBubble from '@/components/MessageBubble.vue'
import ChatInput from '@/components/ChatInput.vue'
import ProjectImportDialog from '@/components/ProjectImportDialog.vue'
import ApprovalCard from '@/components/ApprovalCard.vue'
import * as convApi from '@/api/conversation'
import type { Conversation, ConversationMessage, AgentRunResponse, ToolCallDto } from '@/types/agent'

const route = useRoute()
const router = useRouter()

const conversations = ref<Conversation[]>([])
const activeConversationId = ref<string | null>(null)
const activeWorkspacePath = ref('')
const messages = ref<ConversationMessage[]>([])
const running = ref(false)
const showNewDialog = ref(false)
const pendingApproval = ref<{ approvalId: string; toolName: string; toolArguments: string } | null>(null)
const messageListRef = ref<HTMLElement | null>(null)

const currentUserId = ref('default-user')

const lastAgentMessageIndex = computed(() => {
  for (let i = messages.value.length - 1; i >= 0; i--) {
    if (messages.value[i].role === 'AGENT') return i
  }
  return -1
})

function scrollToBottom(behavior: ScrollBehavior = 'smooth') {
  nextTick(() => {
    const el = messageListRef.value
    if (!el) return
    el.scrollTo({ top: el.scrollHeight, behavior })
  })
}

async function loadConversations() {
  try {
    const res = await convApi.listConversations(currentUserId.value)
    conversations.value = res.data
  } catch {
    // ignore
  }
}

async function selectConversation(id: string) {
  activeConversationId.value = id
  pendingApproval.value = null
  router.push(`/chat/${id}`)
  try {
    const msgs = await convApi.getMessages(id)
    messages.value = msgs.data
  } catch {
    messages.value = []
  }
  const conv = conversations.value.find(c => c.id === id)
  activeWorkspacePath.value = conv?.workspacePath || ''
  scrollToBottom('auto')
}

async function handleCreateConversation(workspacePath: string) {
  showNewDialog.value = false
  try {
    const res = await convApi.createConversation({
      userId: currentUserId.value,
      workspacePath: workspacePath || undefined,
    })
    await loadConversations()
    router.push(`/chat/${res.data.id}`)
    activeConversationId.value = res.data.id
    activeWorkspacePath.value = res.data.workspacePath || ''
    messages.value = []
    pendingApproval.value = null
    scrollToBottom('auto')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || 'Failed to create conversation')
  }
}

async function handleSend(text: string, appendUserMessage = true) {
  if (!activeConversationId.value) return
  running.value = true

  if (appendUserMessage) {
    const userMsg: ConversationMessage = {
      id: Date.now().toString(),
      conversationId: activeConversationId.value,
      sessionId: null,
      role: 'USER',
      content: text,
      toolCalls: [],
      createdAt: new Date().toISOString(),
    }
    messages.value.push(userMsg)
  }
  scrollToBottom()

  try {
    const res = await convApi.sendChat(activeConversationId.value, {
      userId: currentUserId.value,
      message: text,
    })
    handleAgentResponse(res.data)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || 'Agent run failed')
    running.value = false
  }
}

function handleAgentResponse(resp: AgentRunResponse) {
  if (resp.status === 'WAITING_APPROVAL' && resp.pendingApprovalId) {
    pendingApproval.value = {
      approvalId: resp.pendingApprovalId,
      toolName: resp.pendingToolName || 'unknown',
      toolArguments: resp.pendingToolArguments || '{}',
    }
    running.value = false
    scrollToBottom()
    return
  }

  const agentMsg: ConversationMessage = {
    id: Date.now().toString() + '-agent',
    conversationId: activeConversationId.value!,
    sessionId: resp.sessionId,
    role: 'AGENT',
    content: resp.finalAnswer || 'Task completed.',
    toolCalls: resp.toolCalls || [],
    createdAt: new Date().toISOString(),
  }
  messages.value.push(agentMsg)
  pendingApproval.value = null
  running.value = false
  scrollToBottom()
}

async function handleApprove(approvalId: string, reason: string) {
  if (!activeConversationId.value) return
  running.value = true
  pendingApproval.value = null
  try {
    const res = await convApi.resumeConversation(activeConversationId.value, {
      approvalId,
      approved: true,
      reason,
    })
    handleAgentResponse(res.data)
  } catch (e: any) {
    ElMessage.error('Resume failed')
    running.value = false
  }
}

async function handleReject(approvalId: string, reason: string) {
  if (!activeConversationId.value) return
  running.value = true
  pendingApproval.value = null
  try {
    const res = await convApi.resumeConversation(activeConversationId.value, {
      approvalId,
      approved: false,
      reason,
    })
    handleAgentResponse(res.data)
  } catch (e: any) {
    ElMessage.error('Resume failed')
    running.value = false
  }
}

async function handleCopyMessage(content: string) {
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}

async function handleRegenerate() {
  if (running.value || pendingApproval.value) return
  const lastUser = [...messages.value].reverse().find(msg => msg.role === 'USER')
  if (!lastUser) {
    ElMessage.warning('没有可重新生成的用户消息')
    return
  }

  const lastAgentIndex = lastAgentMessageIndex.value
  if (lastAgentIndex >= 0) {
    messages.value.splice(lastAgentIndex, 1)
  }

  await handleSend(lastUser.content, false)
}

async function handleDeleteConversation(id: string) {
  try {
    await ElMessageBox.confirm('Delete this conversation?', 'Confirm', {
      confirmButtonText: 'Delete',
      cancelButtonText: 'Cancel',
      type: 'warning',
    })
    await convApi.deleteConversation(id)
    if (activeConversationId.value === id) {
      activeConversationId.value = null
      messages.value = []
      router.push('/chat')
    }
    await loadConversations()
  } catch {
    // cancelled
  }
}

onMounted(async () => {
  await loadConversations()
  const id = route.params.conversationId as string
  if (id) {
    selectConversation(id)
  }
})

watch(() => route.params.conversationId, (id) => {
  if (id && typeof id === 'string' && id !== activeConversationId.value) {
    selectConversation(id)
  }
})

watch(
  () => [messages.value.length, running.value, pendingApproval.value?.approvalId],
  () => scrollToBottom()
)
</script>

<style scoped>
.chat-layout {
  display: flex;
  height: 100%;
  width: 100%;
}
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.welcome-hint {
  text-align: center;
  color: #999;
  margin-top: 40px;
}
.workspace-info {
  font-size: 13px;
  color: #67c23a;
  margin-top: 8px;
}
.thinking-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
  font-size: 14px;
  padding: 8px 16px;
}
.chat-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
.placeholder-content {
  text-align: center;
}
.placeholder-content h2 {
  font-size: 28px;
  margin-bottom: 8px;
  color: #303133;
}
.placeholder-content p {
  color: #909399;
  margin-bottom: 24px;
}
</style>
