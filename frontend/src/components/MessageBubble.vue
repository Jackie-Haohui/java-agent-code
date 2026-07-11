<template>
  <div class="message-bubble" :class="[message.role.toLowerCase()]">
    <div class="bubble-content">
      <div class="message-text">{{ message.content }}</div>

      <div v-if="message.toolCalls && message.toolCalls.length > 0" class="tool-calls-inline">
        <el-collapse>
          <el-collapse-item
            v-for="(tc, idx) in message.toolCalls"
            :key="idx"
          >
            <template #title>
              <div class="tool-call-header">
                <el-tag :type="tc.success ? 'success' : 'danger'" size="small">
                  {{ tc.toolName }}
                </el-tag>
                <span class="tc-status">{{ tc.success ? 'Success' : 'Failed' }}</span>
              </div>
            </template>
            <div class="tc-detail">
              <div class="tc-section">
                <strong>Arguments:</strong>
                <pre>{{ formatJson(tc.arguments) }}</pre>
              </div>
              <div class="tc-section">
                <strong>Result:</strong>
                <pre>{{ tc.result }}</pre>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>

      <div class="message-actions">
        <el-button link size="small" @click="$emit('copy', message.content)">复制</el-button>
        <el-button
          v-if="canRegenerate"
          link
          size="small"
          type="primary"
          @click="$emit('regenerate')"
        >
          重新生成
        </el-button>
      </div>

      <div v-if="message.sessionId" class="session-link">
        Session: {{ message.sessionId }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ConversationMessage } from '@/types/agent'

withDefaults(defineProps<{
  message: ConversationMessage
  canRegenerate?: boolean
}>(), {
  canRegenerate: false,
})

defineEmits<{
  copy: [content: string]
  regenerate: []
}>()

function formatJson(raw: string) {
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}
</script>

<style scoped>
.message-bubble {
  display: flex;
  margin-bottom: 4px;
}
.message-bubble.user {
  justify-content: flex-end;
}
.message-bubble.agent,
.message-bubble.system {
  justify-content: flex-start;
}
.bubble-content {
  max-width: 75%;
  padding: 10px 16px 8px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.6;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
}
.message-bubble.user .bubble-content {
  background: #409eff;
  color: #fff;
}
.message-bubble.agent .bubble-content {
  background: #f5f7fb;
  color: #303133;
}
.message-bubble.system .bubble-content {
  background: #fdf6ec;
  color: #909399;
  font-style: italic;
}
.message-text {
  white-space: pre-wrap;
  word-break: break-word;
}
.tool-calls-inline {
  margin-top: 8px;
}
.tool-call-header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.tc-status {
  font-size: 12px;
  color: #909399;
}
.tc-detail {
  font-size: 12px;
}
.tc-section {
  margin-bottom: 8px;
}
.tc-section strong {
  display: block;
  margin-bottom: 2px;
}
.tc-section pre {
  background: #fff;
  padding: 6px 10px;
  border-radius: 4px;
  font-size: 11px;
  max-height: 200px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
.message-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  opacity: 0;
  transition: opacity 0.15s ease;
}
.bubble-content:hover .message-actions {
  opacity: 1;
}
.message-bubble.user .message-actions :deep(.el-button) {
  color: rgba(255, 255, 255, 0.9);
}
.session-link {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 6px;
  text-align: right;
}
</style>
